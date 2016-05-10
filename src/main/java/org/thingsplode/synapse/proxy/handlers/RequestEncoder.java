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
package org.thingsplode.synapse.proxy.handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.util.HashSet;
import java.util.List;
import org.thingsplode.synapse.core.AbstractMessage;
import org.thingsplode.synapse.core.Request;
import org.thingsplode.synapse.proxy.RequestDecorator;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
@ChannelHandler.Sharable
public class RequestEncoder extends MessageToMessageEncoder<Request> {

    private final HashSet<RequestDecorator> decorators;

    public RequestEncoder(HashSet<RequestDecorator> decorators, String hostExpression) {
        if (decorators != null) {
            this.decorators = decorators;
        } else {
            this.decorators = new HashSet<>();
        }
        this.decorators.add((RequestDecorator) (Request request) -> {
            //basic data decorator
            request.getHeader().addProperty(HttpHeaderNames.HOST.toString(), hostExpression);
            request.getHeader().addProperty(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.KEEP_ALIVE.toString());
            request.getHeader().addProperty(HttpHeaderNames.ACCEPT_ENCODING.toString(), HttpHeaderValues.GZIP.toString());
            request.getHeader().addProperty(HttpHeaderNames.ACCEPT.toString(), "*/*");
            request.getHeader().addProperty(HttpHeaderNames.USER_AGENT.toString(), "synapse");
            if (Util.notEmpty(request.getHeader().getMsgId())) {
                request.getHeader().addProperty(AbstractMessage.PROP_MESSAGE_ID, request.getHeader().getMsgId());
            }
        });
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Request msg, List<Object> out) throws Exception {
        decorate(msg);
        out.add(msg);
    }

    private void decorate(Request req) {
        decorators.forEach(d -> d.decorate(req));
    }

}
