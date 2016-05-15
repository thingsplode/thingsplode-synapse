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
import io.netty.handler.codec.http.HttpResponseStatus;
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
import org.thingsplode.synapse.core.ConnectionContext;
import org.thingsplode.synapse.core.Request;
import org.thingsplode.synapse.core.exceptions.SerializationException;
import static org.thingsplode.synapse.endpoint.handlers.RequestHandler.CONNECTION_CTX_ATTR;
import org.thingsplode.synapse.serializers.SerializationService;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
public class WebsocketRequestHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketRequestHandler.class);
    private final WebSocketServerHandshaker handshaker;
    private StringBuilder contentBuilder = null;
    private final SerializationService serializationService = SerializationService.getInstance();

    public WebsocketRequestHandler(WebSocketServerHandshaker handShaker) {
        this.handshaker = handShaker;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        try {
            if (frame instanceof CloseWebSocketFrame) {
                handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
                return;
            }

            if (frame instanceof PingWebSocketFrame) {
                updateLastSeen(ctx);
                ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
                return;
            }

            if (frame instanceof PongWebSocketFrame) {
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
                throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
            } else {
                throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
            }

            // Check if Text or Continuation Frame is final fragment and handle if needed.
            if (frame.isFinalFragment()) {
                AbstractMessage synapseMessage = serializationService.getPreferredSerializer(null).unMarshall(AbstractMessage.class, contentBuilder.toString());
                synapseMessage.addProperty(AbstractMessage.PROP_RCV_TRANSPORT, AbstractMessage.PROP_WS_TRANSPORT);
                contentBuilder = null;
                if (synapseMessage instanceof Request) {
                    ((Request) synapseMessage).getHeader().setKeepalive(true);
                }
                ctx.fireChannelRead(synapseMessage);
            }
        } catch (UnsupportedOperationException | SerializationException th) {
            logger.error(th.getClass().getSimpleName() + "error processing ws frame: " + th.getMessage(), th);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(cause.getClass().getSimpleName() + " -> Unhandled Error while processing websocket request: " + cause.getMessage(), cause);
        ResponseEncoder.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

    private void updateLastSeen(ChannelHandlerContext ctx) {
        ConnectionContext cctx = ctx.channel().attr(CONNECTION_CTX_ATTR).get();
        if (cctx != null) {
            cctx.updateLastSeen();
        }
    }
}
