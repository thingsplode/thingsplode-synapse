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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.domain.FileRequest;
import org.thingsplode.synapse.core.domain.HttpStatus;
import org.thingsplode.synapse.core.domain.MediaType;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.exceptions.SynapseException;
import org.thingsplode.synapse.endpoint.ServiceRegistry;

/**
 *
 * @author Csaba Tamas
 */
public class RequestHandler extends SimpleChannelInboundHandler<Request> {

    private final ServiceRegistry registry;
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    public RequestHandler(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {
        Response response;
        try {
            if (request.getBody() == null || request.getBody() instanceof ByteBuf) {
                ByteBuf content = (ByteBuf) request.getBody();
                byte[] dst = new byte[content.capacity()];
                content.getBytes(0, dst);
                String json = new String(dst, Charset.forName("UTF-8"));
                response = registry.invokeWithParsable(request.getHeader(), json);
            } else {
                response = new Response(new Response.ResponseHeader(request.getHeader(), HttpResponseStatus.valueOf(HttpStatus.BAD_REQUEST.value()), new MediaType("text/plain; charset=UTF-8")), "Body type not supported.");
            }
        } catch (SynapseException ex) {
            logger.error("Error processing REST request: " + ex.getMessage(), ex);
            response = new Response(new Response.ResponseHeader(request.getHeader(), HttpResponseStatus.valueOf(ex.getResponseStatus().value()), new MediaType("text/plain; charset=UTF-8")), ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }

        if (response.getHeader().getResponseCode() == HttpResponseStatus.NOT_FOUND) {
            FileRequest fr = new FileRequest(request.getHeader());
            ctx.fireChannelRead(fr);
        } else {
            ctx.fireChannelRead(response);
        }
    }

}
