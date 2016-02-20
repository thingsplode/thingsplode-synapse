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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class HttRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        // Handle a bad request.
        if (!req.decoderResult().isSuccess()) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }
        //method: GET uri: /this_service/call?parameter=1 version: HTTP/1.1
  
         // Host : localhost:8080
        //Connection : keep-alive
        //Cache-Control : max-age=0
        //Accept : text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
        // Upgrade-Insecure-Requests : 1
        //User-Agent : Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/48.0.2564.82 Chrome/48.0.2564.82 Safari/537.36
        //Accept-Encoding : gzip, deflate, sdch
        //Accept-Language : en-US,en;q=0.8
        // content-length : 0
        System.out.println("method: " + req.method().name() + " uri: " + req.uri() + " version: " + req.protocolVersion());
        System.out.println("headers: ");
        req.headers().entries().stream().forEach((entry) -> {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        });

        // check for websocket upgrade request
        String upgradeHeader = req.headers().get("Upgrade");
        if (upgradeHeader != null && "websocket".equalsIgnoreCase(upgradeHeader)) {
            // Handshake. Ideally you'd want to configure your websocket uri
            String url = "ws://" + req.headers().get("Host") + "/wsticker";
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(url, null, false);
            handshaker = wsFactory.newHandshaker(req);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), req);
            }
        } else {
            //boolean handled = handleREST(ctx, req);
            boolean handled = false;
            if (!handled) {
                //httpFileHandler.sendFile(ctx, req);
            }
        }

    }

    public void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    }
}
