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
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;
import org.thingsplode.synapse.core.MediaType;
import org.thingsplode.synapse.core.Request;
import org.thingsplode.synapse.core.Response;

/**
 *
 * @author Csaba Tamas
 */
public class ResponseEncoder extends MessageToMessageEncoder<Response> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Response msg, List<Object> out) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String errorMsg) {
        sendError(ctx, status, errorMsg, null);
    }

    public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String errorMsg, Request request) {
        Response response;
        if (request != null) {
            response = new Response(new Response.ResponseHeader(request.getHeader(), status, MediaType.APPLICATION_JSON));
        } else {
            response = new Response(new Response.ResponseHeader(status));
            response.getHeader().setContentType(MediaType.APPLICATION_JSON);
        }
        response.setBody(errorMsg);
        ctx.fireChannelRead(response);
    }
}
