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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
@ChannelHandler.Sharable
public class ResponseIntrospector extends ChannelOutboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(HttpRequestIntrospector.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg == null) {
            logger.warn("Message@Endpoint <to be sent> is NULL.");
        } else if (logger.isDebugEnabled() && ((msg instanceof HttpResponse) || (msg instanceof HttpContent))) {
            if (!(msg instanceof HttpResponse)) {
                HttpContent content = (HttpContent) msg;
                logger.debug("Message@Endpoint to be sent: http content -> " + content.toString());
            } else {
                final StringBuilder hb = new StringBuilder();
                ((HttpResponse) msg).headers().entries().forEach(e -> {
                    hb.append(e.getKey()).append(" : ").append(e.getValue()).append("\n");
                });
                String payloadAsSring = null;
                if (msg instanceof FullHttpResponse) {
                    ByteBuf content = ((FullHttpResponse) msg).content();
                    byte[] dst = content.copy().array();
                    //content.copy().getBytes(0, dst);
                    content.retain();
                    payloadAsSring = new String(dst, Charset.forName("UTF-8"));
                }
                String msgStatus = (((HttpResponse) msg).status() != null ? ((HttpResponse) msg).status().toString() : "NULL");
                logger.debug("Message@Endpoint to be sent: \n\n"
                        + "Status: " + msgStatus + "\n"
                        + hb.toString() + "\n" + "Payload: [" + (!Util.isEmpty(payloadAsSring) ? payloadAsSring : "EMPTY") + "]\n");
            }
        } else if (logger.isDebugEnabled() && (msg instanceof WebSocketFrame)) {
            if (msg instanceof TextWebSocketFrame) {
                logger.debug("Message@Endpoint to be sent: " + msg.getClass().getSimpleName() + " \n\n" + ((TextWebSocketFrame) msg).text());
            } else {
                logger.debug("Message@Endpoint to be sent: \n\n {" + msg.getClass().getSimpleName() + " -> " + msg.toString() + "}");
            }
        } else {
            logger.debug("Unknown message (" + msg.getClass().getSimpleName() + ") will be dispatched to the client.");
        }
        ctx.write(msg, promise);
    }
}
