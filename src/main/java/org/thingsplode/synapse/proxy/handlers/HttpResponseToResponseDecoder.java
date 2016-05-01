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
package org.thingsplode.synapse.proxy.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.domain.AbstractMessage;
import org.thingsplode.synapse.core.domain.MediaType;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.domain.ResponseBodyWrapper;
import org.thingsplode.synapse.proxy.EndpointProxy;

/**
 * The Handler of an HTTP Response, response initiated by a client request;
 *
 * @author Csaba Tamas
 */
@ChannelHandler.Sharable
public class HttpResponseToResponseDecoder extends MessageToMessageDecoder<HttpResponse> {

    private static final Logger logger = LoggerFactory.getLogger(HttpResponseToResponseDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpResponse httpResponse, List<Object> out) throws Exception {
        if (!httpResponse.decoderResult().isSuccess()) {
            logger.warn("HTTP Resonse decoding was not succesfull.");
        }
        //todo: convert to commands too
        Response rsp = new Response(new Response.ResponseHeader(httpResponse.status()));
        rsp.getHeader().addAllProperties(httpResponse.headers().entries());
        rsp.getHeader().setMsgId(httpResponse.headers().get(AbstractMessage.PROP_MESSAGE_ID));
        rsp.getHeader().setCorrelationId(httpResponse.headers().get(AbstractMessage.PROP_CORRELATION_ID));
        rsp.getHeader().setProtocolVersion(httpResponse.headers().get(AbstractMessage.PROP_PROTOCOL_VERSION));
        rsp.getHeader().setContentType(new MediaType(httpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE)));
        rsp.getHeader().setRemoteAddress(ctx.channel().remoteAddress());

        if (httpResponse instanceof FullHttpResponse) {
            ByteBuf contentBuffer = ((FullHttpResponse) httpResponse).content();
            byte[] dst = new byte[contentBuffer.capacity()];
            contentBuffer.getBytes(0, dst);
            String jsonResponse = new String(dst, Charset.forName("UTF-8"));

            Object o = EndpointProxy.SERIALIZATION_SERVICE.getSerializer(getMediaType(httpResponse)).unMarshall(ResponseBodyWrapper.class, jsonResponse);
            if (o != null && (o instanceof ResponseBodyWrapper)) {
                rsp.setBody(((ResponseBodyWrapper) o).getBody());
            }
        }
        out.add(rsp);
    }

    private MediaType getMediaType(HttpResponse response) {
        String mediaType = getHeaderValue(response, HttpHeaderNames.CONTENT_TYPE.toString()).orElse(MediaType.APPLICATION_JSON);
        return new MediaType(mediaType);
    }

    private Optional<String> getHeaderValue(HttpResponse response, String headerName) {
        return Optional.ofNullable(response.headers().get(headerName));
    }
}
