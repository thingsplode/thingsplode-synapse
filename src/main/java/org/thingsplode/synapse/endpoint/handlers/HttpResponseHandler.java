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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.omg.PortableInterceptor.LOCATION_FORWARD;
import org.thingsplode.synapse.core.domain.MediaType;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.endpoint.serializers.SerializationService;
import org.thingsplode.synapse.endpoint.serializers.SynapseSerializer;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class HttpResponseHandler extends SimpleChannelInboundHandler<Response> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpResponseHandler.class);

    private final SerializationService serializationService = new SerializationService();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response rsp) throws Exception {
        MediaType mt = rsp.getHeader().getContentType();
        SynapseSerializer<String> serializer = serializationService.getSerializer(mt);
        ByteBuf rspBuf = Unpooled.copiedBuffer(serializer.marshall(rsp.getBody()), CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, rsp.getHeader().getResponseCode(), rspBuf);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mt != null ? mt.getName() : "application/json; charset=UTF-8");
        //response.headers().set(HttpHeaderNames.CONTENT_LENGTH, rspBuf.array().length);
        // Close the connection as soon as the message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        //todo: keep alive?
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Error while preparing response: " + cause.getMessage(), cause);
        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

    public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String errorMsg) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + errorMsg + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    static void sendRedirect(ChannelHandlerContext ctx, String newUrl) {
        logger.debug("Redirecting request to {}", newUrl);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.TEMPORARY_REDIRECT, Unpooled.copiedBuffer("redirect to index.html", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.LOCATION, newUrl);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

}
