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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.Optional;
import org.thingsplode.synapse.core.domain.FileRequest;
import org.thingsplode.synapse.core.domain.MediaType;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.domain.Uri;
import org.thingsplode.synapse.core.exceptions.SynapseException;
import org.thingsplode.synapse.endpoint.ServiceRegistry;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final String MSG_ID_HEADER_KEY = "Msg-Id";
    private static final String HVALUE_UPGRADE = "Upgrade";
    private final String endpointId;
    private WebSocketServerHandshaker handshaker;
    private final ServiceRegistry registry;
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpRequestHandler.class);

    public HttpRequestHandler(String endpointId, ServiceRegistry registry) {
        this.registry = registry;
        this.endpointId = endpointId;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
        try {
            // Handle a bad request.
            if (!httpRequest.decoderResult().isSuccess()) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST, "Could not decode request.");
                return;
            }

            if (httpRequest.method() == HttpMethod.HEAD
                    || httpRequest.method() == HttpMethod.PATCH
                    || httpRequest.method() == HttpMethod.TRACE
                    || httpRequest.method() == HttpMethod.CONNECT
                    || httpRequest.method() == HttpMethod.OPTIONS) {
                sendError(ctx, HttpResponseStatus.FORBIDDEN, "Method forbidden (The following are not supported: HEAD, PATCH, TRACE, CONNECT, OPTIONS).");
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
                Request.RequestHeader header = null;
                Optional<Entry<String, String>> msgIdOpt = httpRequest.headers().entries().stream().filter(e -> e.getKey().equalsIgnoreCase(MSG_ID_HEADER_KEY)).findFirst();
                String msgId = msgIdOpt.isPresent() ? msgIdOpt.get().getValue() : null;
                try {
                    header = new Request.RequestHeader(msgId, new Uri(httpRequest.uri()), Request.RequestHeader.RequestMethod.fromHttpMethod(httpRequest.method()));
                    header.addAllRequestProperties(httpRequest.headers());
                } catch (UnsupportedEncodingException ex) {
                    //return new Response(new Response.ResponseHeader(UUID.randomUUID().toString(), msgId, HttpResponseStatus.BAD_REQUEST));
                }
                Response response = handleREST(ctx, header, httpRequest.content());
                //https://github.com/RestExpress/RestExpress/blob/master/core/src/test/java/org/restexpress/contenttype/MediaTypeParserTest.java
                //https://github.com/jwboardman/khs-stockticker/blob/master/src/main/java/com/khs/stockticker/StockTickerServerHandler.java
                //https://keyholesoftware.com/2015/03/16/netty-a-different-kind-of-websocket-server/
                //http://microservices.io/patterns/microservices.html
                ctx.fireChannelRead(response);
                if (response.getHeader().getResponseCode() == HttpResponseStatus.NOT_FOUND) {
                    //http://netty.io/4.0/api/io/netty/channel/ChannelPipeline.html
                    FileRequest fr = new FileRequest(header);
                    ctx.fireChannelRead(fr);
                }
            }
        } catch (Exception ex) {
            logger.error("Channel read error: " + ex.getMessage(), ex);
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private Response handleREST(ChannelHandlerContext ctx, Request.RequestHeader header, ByteBuf content) {
        try {
            byte[] dst = new byte[content.capacity()];
            content.getBytes(0, dst);
            String json = new String(dst, Charset.forName("UTF-8"));
            return registry.invokeWithParseable(header, json);
        } catch (SynapseException ex) {
            return new Response(new Response.ResponseHeader(header, HttpResponseStatus.valueOf(ex.getResponseStatus().value()), new MediaType("text/plain; charset=UTF-8")), ex.getMessage());
        }
    }

    public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String errorMsg) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + errorMsg + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    }

}
