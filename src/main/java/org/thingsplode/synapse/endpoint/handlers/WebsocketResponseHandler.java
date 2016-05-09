/*
 * Copyright 2016 Csaba Tamas.
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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.MediaType;
import org.thingsplode.synapse.core.Response;
import org.thingsplode.synapse.serializers.SerializationService;

/**
 *
 * @author Csaba Tamas
 */
public class WebsocketResponseHandler extends SimpleChannelInboundHandler<Response> {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketResponseHandler.class);
    private final SerializationService serializationService = SerializationService.getInstance();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {
        byte[] content = serializationService.getSerializer(MediaType.APPLICATION_JSON).marshall(response);
        TextWebSocketFrame wsFrame = new TextWebSocketFrame(Unpooled.wrappedBuffer(content));
        ctx.writeAndFlush(wsFrame).addListener((ChannelFutureListener) new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    logger.error("Closing channel due to an error while sending response to websocket client -> " + (future.cause() != null ? future.cause().getMessage() : "unknown reason."));
                    ctx.channel().close();
                }
            }
        });
    }

}
