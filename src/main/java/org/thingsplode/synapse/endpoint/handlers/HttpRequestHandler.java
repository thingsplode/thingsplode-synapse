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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import java.io.UnsupportedEncodingException;
import java.util.Map.Entry;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.domain.AbstractMessage;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.RequestMethod;
import org.thingsplode.synapse.core.domain.Uri;
import org.thingsplode.synapse.endpoint.ServiceRegistry;
import static org.thingsplode.synapse.endpoint.handlers.HttpResponseHandler.sendError;
import org.thingsplode.synapse.util.Util;

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
    private static final String HVALUE_UPGRADE = "Upgrade";
    private final String endpointId;
    private WebSocketServerHandshaker handshaker;

    public HttpRequestHandler(String endpointId) {
        this.endpointId = endpointId;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
        //todo: support for API keys
        ///endpoints/json?api_key=565656
        try {
            // Handle a bad request.
            if (!httpRequest.decoderResult().isSuccess()) {
                HttpResponseHandler.sendError(ctx, HttpResponseStatus.BAD_REQUEST, "Could not decode request.");
                return;
            }

            if (httpRequest.method() == HttpMethod.HEAD
                    || httpRequest.method() == HttpMethod.PATCH
                    || httpRequest.method() == HttpMethod.TRACE
                    || httpRequest.method() == HttpMethod.CONNECT
                    || httpRequest.method() == HttpMethod.OPTIONS) {
                HttpResponseHandler.sendError(ctx, HttpResponseStatus.FORBIDDEN, "Method forbidden (The following are not supported: HEAD, PATCH, TRACE, CONNECT, OPTIONS).");
                return;
            }

            String upgradeHeader = httpRequest.headers().get(HVALUE_UPGRADE);
            if (!Util.isEmpty(upgradeHeader) && "websocket".equalsIgnoreCase(upgradeHeader)) {
                // Handshake. Ideally you'd want to configure your websocket uri
                String url = "ws://" + httpRequest.headers().get("Host") + "/" + endpointId;
                //todo: configure frame size
                WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(url, null, false);
                handshaker = wsFactory.newHandshaker(httpRequest);
                if (handshaker == null) {
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    handshaker.handshake(ctx.channel(), httpRequest);
                }
            } else {
                //simple http request
                Request.RequestHeader header = null;
                Optional<Entry<String, String>> msgIdOpt = httpRequest.headers().entries().stream().filter(e -> e.getKey().equalsIgnoreCase(AbstractMessage.PROP_MESSAGE_ID)).findFirst();
                String msgId = msgIdOpt.isPresent() ? msgIdOpt.get().getValue() : null;
                try {
                    header = new Request.RequestHeader(msgId, new Uri(httpRequest.uri()), RequestMethod.fromHttpMethod(httpRequest.method()));
                    header.addAllMessageProperties(httpRequest.headers());
                    if (HttpHeaders.isKeepAlive(httpRequest)) {
                        header.setKeepalive(true);
                    } else {
                        header.setKeepalive(false);
                    }
                } catch (UnsupportedEncodingException ex) {
                    logger.error(ex.getMessage(), ex);
                    HttpResponseHandler.sendError(ctx, HttpResponseStatus.BAD_REQUEST, ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
                Request request = new Request(header);
                request.setBody(httpRequest.content());
                ctx.fireChannelRead(request);
            }
        } catch (Exception ex) {
            logger.error("Channel read error: " + ex.getMessage(), ex);
            HttpResponseHandler.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Error while processing HTTP request: " + cause.getMessage(), cause);
        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

}
