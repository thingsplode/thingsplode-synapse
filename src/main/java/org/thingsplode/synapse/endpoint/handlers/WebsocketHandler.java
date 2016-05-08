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
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.AbstractMessage;
import org.thingsplode.synapse.serializers.SerializationService;

/**
 *
 * @author Csaba Tamas
 */
public class WebsocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final WebSocketServerHandshaker handshaker;
    private StringBuilder contentBuilder = null;
    private static final Logger logger = LoggerFactory.getLogger(WebsocketHandler.class);
    private final SerializationService serializationService = new SerializationService();

    public WebsocketHandler(WebSocketServerHandshaker handShaker) {
        this.handshaker = handShaker;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {

        if (frame instanceof CloseWebSocketFrame) {
            //ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if (frame instanceof PongWebSocketFrame) {
            logger.info("Pong frame received");
            return;
        }

        if (frame instanceof TextWebSocketFrame) {
            contentBuilder = new StringBuilder(((TextWebSocketFrame) frame).text());
        } else if (frame instanceof ContinuationWebSocketFrame) {
            if (contentBuilder != null) {
                contentBuilder.append(((ContinuationWebSocketFrame) frame).text());
            } else {
                logger.warn("Continuation frame received without initial frame.");
            }
        } else if (frame instanceof BinaryWebSocketFrame) {
            //todo: extend with support for it;
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        } else {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        }

        // Check if Text or Continuation Frame is final fragment and handle if needed.
        if (frame.isFinalFragment()) {
            AbstractMessage msg = serializationService.getPreferredSerializer(null).unMarshall(AbstractMessage.class, contentBuilder.toString());
            contentBuilder = null;
            ctx.fireChannelRead(msg);
        }

    }
}
