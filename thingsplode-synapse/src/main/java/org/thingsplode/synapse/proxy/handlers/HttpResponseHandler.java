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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.List;
import org.thingsplode.synapse.core.domain.AbstractMessage;
import org.thingsplode.synapse.core.domain.MediaType;
import org.thingsplode.synapse.core.domain.Response;

/**
 * The Handler of an HTTP Response, response initiated by a client request;
 * @author Csaba Tamas
 */
public class HttpResponseHandler extends MessageToMessageDecoder<HttpResponse> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpResponseHandler.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpResponse httpResponse, List<Object> out) throws Exception {
        if (!httpResponse.decoderResult().isSuccess()) {
            logger.warn("HTTP Resonse decoding was not succesfull.");
        }
        //todo: convert to commands too
        Response rsp = new Response(new Response.ResponseHeader(httpResponse.status()));
        rsp.getHeader().addAllMessageProperties(httpResponse.headers().entries());
        rsp.getHeader().setMsgId(httpResponse.headers().get(AbstractMessage.PROP_MESSAGE_ID));
        rsp.getHeader().setCorrelationId(httpResponse.headers().get(AbstractMessage.PROP_CORRELATION_ID));
        rsp.getHeader().setProtocolVersion(httpResponse.headers().get(AbstractMessage.PROP_PROTOCOL_VERSION));
        rsp.getHeader().setContentType(new MediaType(httpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE)));
        rsp.getHeader().setRemoteAddress(ctx.channel().remoteAddress());
        out.add(rsp);
    }
}
