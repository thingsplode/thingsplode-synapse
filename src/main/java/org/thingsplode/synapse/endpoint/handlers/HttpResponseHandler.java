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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.domain.AbstractMessage;
import org.thingsplode.synapse.core.domain.MediaType;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.domain.ResponseBodyWrapper;
import org.thingsplode.synapse.serializers.SerializationService;
import org.thingsplode.synapse.serializers.SynapseSerializer;

/**
 *
 * @author Csaba Tamas
 */
@ChannelHandler.Sharable
public class HttpResponseHandler extends SimpleChannelInboundHandler<Response> {

    private static final Logger logger = LoggerFactory.getLogger(HttpResponseHandler.class);
    private final SerializationService serializationService = new SerializationService();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response rsp) throws Exception {
        MediaType mt = rsp.getHeader().getContentType();
        SynapseSerializer<String> serializer = serializationService.getSerializer(mt);
        byte[] payload = serializer.marshall(new ResponseBodyWrapper<>(rsp.getBody()));
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, rsp.getHeader().getResponseCode(), Unpooled.wrappedBuffer(payload));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mt != null ? mt.getName() : "application/json; charset=UTF-8");
        decorate(rsp, response);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        if (!rsp.getHeader().isKeepAlive()) {
            // If keep-alive is off, close the connection once the content is fully written.
            logger.trace("Closing the Connection@Endpoint due to keep-alive: false.");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        }
    }

    private void decorate(Response rsp, HttpResponse httpResponse) {
        rsp.getHeader().getMessageProperties().keySet().stream().forEach(k -> {
            httpResponse.headers().set(k, rsp.getHeader().getMessageProperty(k));
        });
        if (rsp.getHeader().getCorrelationId() != null) {
            httpResponse.headers().set(AbstractMessage.PROP_CORRELATION_ID, rsp.getHeader().getCorrelationId());
        }
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
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE).addListener((ChannelFutureListener) (ChannelFuture future) -> {
            Throwable th = future.cause();
            if (th != null) {
                logger.error("Sending response from Endpoint was not successful: " + th.getMessage(), th);
            }
            if (logger.isDebugEnabled() && future.isSuccess()) {
                logger.debug("Response message was succesfully dispatched at the endpoint.");
            }
        });
    }

    static void sendRedirect(ChannelHandlerContext ctx, String newUrl) {
        logger.debug("Redirecting request to {}", newUrl);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.TEMPORARY_REDIRECT, Unpooled.copiedBuffer("redirect to index.html", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.LOCATION, newUrl);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

}
