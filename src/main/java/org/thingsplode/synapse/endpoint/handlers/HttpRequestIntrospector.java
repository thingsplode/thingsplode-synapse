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
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
@ChannelHandler.Sharable
public class HttpRequestIntrospector extends SimpleChannelInboundHandler<HttpRequest> {

    private Logger logger = LoggerFactory.getLogger(HttpRequestIntrospector.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        try {
            if (msg == null) {
                logger.warn("Message@Endpoint received: NULL");
            } else if (logger.isDebugEnabled()) {
                final StringBuilder hb = new StringBuilder();
                msg.headers().entries().forEach(e -> {
                    hb.append(e.getKey()).append(" : ").append(e.getValue()).append("\n");
                });
                String payloadAsSring = null;
                if (msg instanceof FullHttpRequest) {
                    ByteBuf content = ((FullHttpRequest) msg).content();
                    byte[] dst = new byte[content.capacity()];
                    content.copy().getBytes(0, dst);
                    content.retain();
                    payloadAsSring = new String(dst, Charset.forName("UTF-8"));
                }
                logger.debug("Message@Endpoint received: \n\n"
                        + "Uri: " + msg.uri() + "\n"
                        + "Method: " + msg.method() + "\n"
                        + hb.toString() + "\n" + "Payload -> " + (!Util.isEmpty(payloadAsSring) ? payloadAsSring + "\n" : "EMPTY\n"));
            }
        } catch (Throwable th) {
            logger.error(th.getClass().getSimpleName() + " caught while introspecting request, with message: " + th.getMessage(), th);
        }
        if (msg != null && msg instanceof FullHttpRequest) {
            ((FullHttpRequest) msg).content().retain();
        }
        ctx.fireChannelRead(msg);
    }
}
