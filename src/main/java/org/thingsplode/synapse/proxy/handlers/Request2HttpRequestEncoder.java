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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import java.util.List;
import java.util.Optional;
import org.thingsplode.synapse.core.AbstractMessage;
import org.thingsplode.synapse.core.MediaType;
import org.thingsplode.synapse.core.Request;
import org.thingsplode.synapse.core.exceptions.SerializationException;
import org.thingsplode.synapse.proxy.EndpointProxy;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
@ChannelHandler.Sharable
public class Request2HttpRequestEncoder extends MessageToMessageEncoder<Request> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Request in, List<Object> out) throws Exception {
        HttpRequest outMsg = convert(in);
        out.add(outMsg);
    }

    private HttpRequest convert(Request request) throws SerializationException {
        if (request == null || request.getHeader() == null) {
            return null;
        }
        HttpMethod m = null;
        if (request.getHeader().getMethod() != null) {
            m = HttpMethod.valueOf(request.getHeader().getMethod().toString());
        } else if (request.getHeader().getUri() != null && !Util.isEmpty(request.getHeader().getUri().getQuery())) {
            m = HttpMethod.GET;
        } else if (request.getBody() != null) {
            m = HttpMethod.PUT;
        } else {
            m = HttpMethod.GET;
        }
        final HttpRequest out;
        if (request.getBody() != null) {
            Optional<String> contentTypeOpt = request.getRequestHeaderProperty(HttpHeaderNames.ACCEPT.toString());
            MediaType mt = contentTypeOpt.isPresent() ? new MediaType(contentTypeOpt.get()) : new MediaType(MediaType.APPLICATION_JSON_CT);
            request.getHeader().addProperty(AbstractMessage.PROP_BODY_TYPE, request.getBody().getClass().getCanonicalName());
            byte[] payload = EndpointProxy.SERIALIZATION_SERVICE.getSerializer(mt).marshall(request.getBody());
            out = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, m, request.getHeader().getUri().getPath(), Unpooled.wrappedBuffer(payload));
            out.headers().add(HttpHeaderNames.CONTENT_LENGTH, payload.length);
        } else {
            out = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, m, request.getHeader().getUri().getPath());
        }
        if (request.getHeader().isKeepalive()) {
            out.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        if (request.getHeader() != null && request.getHeader().getProperties() != null) {
            request.getHeader().getProperties().forEach((k, v) -> {
                out.headers().set(new AsciiString(k), new AsciiString(v != null ? v : ""));
            });
        }
        return out;
    }
}
