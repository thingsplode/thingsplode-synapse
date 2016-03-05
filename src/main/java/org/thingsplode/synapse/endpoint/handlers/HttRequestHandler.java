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
import org.thingsplode.synapse.endpoint.ServiceRegistry;
import org.thingsplode.synapse.endpoint.UriHandlingCapable;
import org.thingsplode.synapse.endpoint.handlers.internal.FileHandler;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class HttRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> implements UriHandlingCapable {

    private static final String HVALUE_UPGRADE = "Upgrade";
    private final String endpointId;
    private final FileHandler fileHandler;
    private WebSocketServerHandshaker handshaker;
    private ServiceRegistry registry;
    

    public HttRequestHandler(String endpointId, FileHandler fileHandler) {
        this.endpointId = endpointId;
        this.fileHandler = fileHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
        // Handle a bad request.
        if (!httpRequest.decoderResult().isSuccess()) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        if (httpRequest.method() == HttpMethod.HEAD
                || httpRequest.method() == HttpMethod.PATCH
                || httpRequest.method() == HttpMethod.TRACE
                || httpRequest.method() == HttpMethod.CONNECT
                || httpRequest.method() == HttpMethod.OPTIONS) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN);
            return;
        }

        //Uri uri = new Uri(httpRequest.uri());
        //Optional<InternalServiceRegistry.MethodContext> mcOpt = registry.matchUri(req.method(), uri);
        //Request req = new Request(new Request.RequestHeader(uri, RequestMethod.fromHttpMethod(httpRequest.getMethod())), httpRequest.content());
        //registry.invoke(RequestMethod.GET, uri, registry)
        //---------------------------------
        // check for websocket upgrade request
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
            boolean handled = handleREST(ctx, httpRequest);
            if (!handled) {
                //httpFileHandler.sendFile(ctx, req);
            }
        }

    }

    private boolean handleREST(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        return false;
    }

    public void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    }

    @Override
    public void setServiceRegistry(ServiceRegistry registry) {
        this.registry = registry;
    }
}
