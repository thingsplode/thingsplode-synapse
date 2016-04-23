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
import io.netty.handler.codec.http.HttpResponse;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
@ChannelHandler.Sharable
public class HttpResponseIntrospector extends ChannelOutboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(HttpRequestIntrospector.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg == null || !(msg instanceof HttpResponse)) {
            logger.warn("Message@Endpoint <to be sent>: not an instance of HTTPResponse or NULL.");
        } else if (logger.isDebugEnabled()) {
            final StringBuilder hb = new StringBuilder();
            ((HttpResponse) msg).headers().entries().forEach(e -> {
                hb.append(e.getKey()).append(" : ").append(e.getValue()).append("\n");
            });
            String payloadAsSring = null;
            if (msg instanceof FullHttpResponse) {
                ByteBuf content = ((FullHttpResponse) msg).content();
                byte[] dst = new byte[content.capacity()];
                content.copy().getBytes(0, dst);
                content.retain();
                payloadAsSring = new String(dst, Charset.forName("UTF-8"));
            }
            logger.debug("Message@Endpoint to be sent: \n\n"
                    + "Status: " + ((HttpResponse) msg).status() + "\n"
                    + hb.toString() + "\n" + "Payload: [" + (!Util.isEmpty(payloadAsSring) ? payloadAsSring + "]\n" : "EMPTY\n"));
        }
        ctx.write(msg, promise);
    }
}
