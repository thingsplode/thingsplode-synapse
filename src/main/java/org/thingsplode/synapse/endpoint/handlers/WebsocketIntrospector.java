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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Csaba Tamas
 */
public class WebsocketIntrospector extends SimpleChannelInboundHandler<WebSocketFrame> {

    private Logger logger = LoggerFactory.getLogger(WebsocketIntrospector.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        try {
            if (msg == null) {
                logger.warn("Message@Endpoint received: NULL");
            } else if (logger.isDebugEnabled()) {
                if (msg instanceof TextWebSocketFrame) {
                    logger.debug("WebsocketFrame@Endpoint received: \n\n" + ((TextWebSocketFrame) msg).text());
                } else {
                    logger.debug("WebsocketFrame@Endpoint of type [" + msg.getClass().getCanonicalName() + "] received.");
                }
            }
        } catch (Throwable th) {
            logger.error(th.getClass().getSimpleName() + " caught while introspecting request, with message: " + th.getMessage(), th);
        }
        if (msg != null) {
            msg.retain();
        }
        ctx.fireChannelRead(msg);
    }

}
