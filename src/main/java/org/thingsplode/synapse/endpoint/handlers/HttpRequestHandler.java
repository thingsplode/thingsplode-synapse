/*
 * Copyright 2016 tamas.csaba@gmail.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsplode.synapse.endpoint.handlers;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import java.io.UnsupportedEncodingException;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.AbstractMessage;
import org.thingsplode.synapse.core.Request;
import org.thingsplode.synapse.core.RequestMethod;
import org.thingsplode.synapse.core.Uri;
import org.thingsplode.synapse.endpoint.Endpoint;
import org.thingsplode.synapse.util.Util;
import static org.thingsplode.synapse.endpoint.handlers.HttpResponseHandler.sendError;

/**
 *
 * @author Csaba Tamas
 */
//todo: cleanup the content from below
//https://github.com/RestExpress/RestExpress/blob/master/core/src/test/java/org/restexpress/contenttype/MediaTypeParserTest.java
//https://github.com/jwboardman/khs-stockticker/blob/master/src/main/java/com/khs/stockticker/StockTickerServerHandler.java
//https://keyholesoftware.com/2015/03/16/netty-a-different-kind-of-websocket-server/
//http://microservices.io/patterns/microservices.html
//http://netty.io/4.0/api/io/netty/channel/ChannelPipeline.html
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);
    public static final String UPGRADE_TO_WEBSOCKET = "websocket";
    private final String endpointId;
    private boolean pipelining = false;
    private final AtomicLong sequence = new AtomicLong(0);

    public HttpRequestHandler(String endpointId, boolean pipelining) {
        this.endpointId = endpointId;
        this.pipelining = pipelining;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
        //todo: support for API keys
        ///endpoints/json?api_key=565656
        try {
            // Handle a bad request.
            if (!httpRequest.decoderResult().isSuccess()) {
                HttpResponseHandler.sendError(ctx, HttpResponseStatus.BAD_REQUEST, "Could not decode request.", httpRequest);
                return;
            }

            if (httpRequest.method().equals(HttpMethod.HEAD)
                    || httpRequest.method().equals(HttpMethod.PATCH)
                    || httpRequest.method().equals(HttpMethod.TRACE)
                    || httpRequest.method().equals(HttpMethod.CONNECT)
                    || httpRequest.method().equals(HttpMethod.OPTIONS)) {
                HttpResponseHandler.sendError(ctx, HttpResponseStatus.FORBIDDEN, "Method forbidden (The following are not supported: HEAD, PATCH, TRACE, CONNECT, OPTIONS).", httpRequest);
                return;
            }

            //check websocket upgrade request
            String upgradeHeader = httpRequest.headers().get(HttpHeaderNames.UPGRADE);
            if (!Util.isEmpty(upgradeHeader) && UPGRADE_TO_WEBSOCKET.equalsIgnoreCase(upgradeHeader)) {
                //case websocket upgrade request is detected -> Prepare websocket handshake
                upgradeToWebsocket(ctx, httpRequest);
                return;
            } else {
                //case simple http request
                Request request = new Request(prepareHeader(ctx, httpRequest));

                //no information about the object type / it will be processed in a later stage
                //todo: with requestbodytype header value early deserialization would be possible, however not beneficial in routing cases
                request.setBody(httpRequest.content());
                ctx.fireChannelRead(request);
            }
        } catch (Exception ex) {
            logger.error("Channel read error: " + ex.getMessage(), ex);
            HttpResponseHandler.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, ex.getClass().getSimpleName() + ": " + ex.getMessage(), httpRequest);
        }
    }

    private void upgradeToWebsocket(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {

        String wsUrl = "ws://" + httpRequest.headers().get(HttpHeaderNames.HOST) + "/" + endpointId;
        //todo: configure frame size
        WebSocketServerHandshakerFactory wsHandshakeFactory = new WebSocketServerHandshakerFactory(wsUrl, null, false);
        WebSocketServerHandshaker handshaker = wsHandshakeFactory.newHandshaker(httpRequest);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), httpRequest).addListener((ChannelFutureListener) new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        logger.debug("Switching to websocket. Replacing the " + Endpoint.HTTP_RESPONSE_HANDLER + " with " + Endpoint.WS_RESPONSE_HANDLER);
                        ctx.pipeline().replace(Endpoint.HTTP_REQUEST_HANDLER, Endpoint.WS_REQUEST_HANDLER, new WebsocketRequestHandler(handshaker));
                        ctx.pipeline().replace(Endpoint.HTTP_RESPONSE_HANDLER, Endpoint.WS_RESPONSE_HANDLER, new WebsocketResponseHandler());
                        if (ctx.pipeline().get(Endpoint.RESPONSE_INTROSPECTOR) != null) {
                            ctx.pipeline().addAfter(Endpoint.RESPONSE_INTROSPECTOR, Endpoint.WS_REQUEST_INTROSPECTOR, new WebsocketIntrospector());
                        }
                    } else {
                        String msg = "Dispatching upgrade acknowledgement was not successfull due to ";
                        if (future.cause() != null) {
                            logger.error(msg + future.cause().getClass().getSimpleName() + " with message: " + future.cause().getMessage(), future.cause());
                        } else {
                            logger.error(msg + " unknown reasons.");
                        }
                    }
                }
            });
        }
    }

    private Request.RequestHeader prepareHeader(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        Request.RequestHeader header = null;
        Optional<Entry<String, String>> msgIdOpt = httpRequest.headers().entries().stream().filter(e -> e.getKey().equalsIgnoreCase(AbstractMessage.PROP_MESSAGE_ID)).findFirst();
        String msgId = msgIdOpt.isPresent() ? msgIdOpt.get().getValue() : null;
        try {
            header = new Request.RequestHeader(msgId, new Uri(httpRequest.uri()), RequestMethod.fromHttpMethod(httpRequest.method()));
            header.addAllProperties(httpRequest.headers());
            if (HttpHeaders.isKeepAlive(httpRequest)) {
                header.setKeepalive(true);
            } else {
                header.setKeepalive(false);
            }
            if (pipelining) {
                header.addProperty(Request.RequestHeader.MSG_SEQ, String.valueOf(sequence.getAndIncrement()));
            }
            header.addProperty(AbstractMessage.PROP_RCV_CHANNEL, AbstractMessage.PROP_KEY_HTTP);
        } catch (UnsupportedEncodingException ex) {
            logger.error(ex.getMessage(), ex);
            HttpResponseHandler.sendError(ctx, HttpResponseStatus.BAD_REQUEST, ex.getClass().getSimpleName() + ": " + ex.getMessage(), httpRequest);
        }
        return header;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Error while processing HTTP request: " + cause.getMessage(), cause);
        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

}
