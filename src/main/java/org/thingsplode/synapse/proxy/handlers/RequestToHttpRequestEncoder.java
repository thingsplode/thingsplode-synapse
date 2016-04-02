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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import java.util.List;
import java.util.Optional;
import org.thingsplode.synapse.core.domain.MediaType;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.exceptions.SerializationException;
import org.thingsplode.synapse.serializers.SerializationService;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
public class RequestToHttpRequestEncoder extends MessageToMessageEncoder<Request> {

    private final SerializationService serializationService = new SerializationService();

    @Override
    protected void encode(ChannelHandlerContext ctx, Request msg, List<Object> out) throws Exception {
        out.add(convert(msg));
    }

    private HttpRequest convert(Request in) throws SerializationException {
        if (in == null || in.getHeader() == null) {
            return null;
        }
        HttpMethod m = null;
        if (in.getHeader().getMethod() != null) {
            m = HttpMethod.valueOf(in.getHeader().getMethod().toString());
        } else if (in.getHeader().getUri() != null && !Util.isEmpty(in.getHeader().getUri().getQuery())) {
            m = HttpMethod.GET;
        } else if (in.getBody() != null) {
            m = HttpMethod.PUT;
        } else {
            m = HttpMethod.GET;
        }
        final HttpRequest out;
        if (in.getBody() != null) {
            Optional<String> contentTypeOpt = in.getRequestHeaderProperty(HttpHeaderNames.ACCEPT.toString());
            MediaType mt = contentTypeOpt.isPresent() ? new MediaType(contentTypeOpt.get()) : new MediaType(MediaType.APPLICATION_JSON);
            byte[] payload = serializationService.getSerializer(mt).marshall(in.getBody());
            out = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, m, in.getHeader().getUri().getPath(), Unpooled.wrappedBuffer(payload));
        } else {
            out = new DefaultHttpRequest(HttpVersion.HTTP_1_1, m, in.getHeader().getUri().getPath());
        }
        in.getHeader().getRequestProperties().forEach((k, v) -> {
            out.headers().set(new AsciiString(k), new AsciiString(v));
        });
        return out;
    }
}
