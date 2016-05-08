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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.AbstractMessage;
import org.thingsplode.synapse.core.MediaType;
import org.thingsplode.synapse.core.Response;
import org.thingsplode.synapse.core.EmptyBody;
import org.thingsplode.synapse.core.Request;
import org.thingsplode.synapse.serializers.SerializationService;
import org.thingsplode.synapse.serializers.SynapseSerializer;

/**
 * Once the Response is prepared by the business logic, the final touches will
 * be added by this handler and the Synapse specific Response will be converted
 * to an HttpResponse.<br>
 * This handler:
 * <ul>
 * <li> will convert the message body object into a serialized message format
 * (eg. Json);
 * <li> handle keepalive status
 * <li> convert Response Message Properties to HTTP headers
 * <li> prepare additional HTTP specific header values
 * </ul>
 *
 * @author Csaba Tamas
 */
@ChannelHandler.Sharable
public class HttpResponseHandler extends SimpleChannelInboundHandler<Response> {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseHandler.class);
    public static String SEC_WEBSOCKET_KEY_SALT = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private final SerializationService serializationService = new SerializationService();
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response rsp) throws Exception {
        MediaType mt = rsp.getHeader().getContentType();
        SynapseSerializer<String> serializer = serializationService.getSerializer(mt);
        
        if (rsp.getBody() != null) {
            rsp.getHeader().addProperty(AbstractMessage.PROP_BODY_TYPE, rsp.getBody().getClass().getCanonicalName());
        }
        byte[] payload = serializer.marshall(rsp.getBody() != null ? rsp.getBody() : new EmptyBody());
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, rsp.getHeader().getResponseCode(), Unpooled.wrappedBuffer(payload));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mt != null ? mt.getName() : "application/json; charset=UTF-8");
        decorate(rsp, response);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        writeResponseWithKeepaliveHandling(ctx, response, rsp.getHeader().isKeepAlive());
    }
    
    private static ChannelFuture writeResponseWithKeepaliveHandling(ChannelHandlerContext ctx, FullHttpResponse response, boolean keepalive) {
        if (!keepalive) {
            // If keep-alive is off, close the connection once the content is fully written.
            logger.trace("Closing the Connection@Endpoint due to keep-alive: false.");
            return ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            return ctx.writeAndFlush(response);
        }
    }
    
    private void decorate(Response rsp, HttpResponse httpResponse) {
        rsp.getHeader().getProperties().keySet().stream().forEach(k -> {
            Optional<String> headerValueOpt = rsp.getHeader().getProperty(k);
            if (headerValueOpt.isPresent()) {
                httpResponse.headers().set(k, headerValueOpt.get());
            }
        });
        if (rsp.getHeader().getMsgId() != null) {
            httpResponse.headers().set(AbstractMessage.PROP_MESSAGE_ID, rsp.getHeader().getMsgId());
        }
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
        sendError(ctx, status, errorMsg, null);
    }
    
    public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String errorMsg, HttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + errorMsg + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        if (request != null && request.headers().contains(AbstractMessage.PROP_MESSAGE_ID)) {
            response.headers().set(AbstractMessage.PROP_CORRELATION_ID, request.headers().get(AbstractMessage.PROP_MESSAGE_ID));
        }
        writeResponseWithKeepaliveHandling(ctx, response, request != null ? HttpHeaders.isKeepAlive(request) : false).addListener((ChannelFutureListener) (ChannelFuture future) -> {
            Throwable th = future.cause();
            if (th != null) {
                logger.error("Sending response from Endpoint was not successful: " + th.getMessage(), th);
            }
            if (logger.isDebugEnabled() && future.isSuccess()) {
                logger.debug("Error Response message was succesfully dispatched at the endpoint.");
            }
        });
    }
    
    static void sendWebsocketUpgradeResponse(ChannelHandlerContext ctx, HttpRequest request) {
        ByteBuf content = Unpooled.copiedBuffer("upgrading to websocket...", CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.SWITCHING_PROTOCOLS, content);
        response.headers().set(HttpHeaderNames.UPGRADE, HttpRequestHandler.UPGRADE_TO_WEBSOCKET);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.UPGRADE);
        if (request != null && request.headers().contains(HttpHeaderNames.SEC_WEBSOCKET_KEY)) {
            try {
                //prepare a salt-ed, hashed base64 encoded accept challenge as requested by the specification
                String secWebsocketKey = request.headers().get(HttpHeaderNames.SEC_WEBSOCKET_KEY) + SEC_WEBSOCKET_KEY_SALT;
                MessageDigest cript = MessageDigest.getInstance("SHA-1");
                cript.reset();
                cript.update(secWebsocketKey.getBytes("utf8"));
                String base64EncodedHashedSecWsAccept = Base64.getEncoder().encodeToString(cript.digest());
                response.headers().set(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT, base64EncodedHashedSecWsAccept);
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
                logger.warn("Should prepare a [" + HttpHeaderNames.SEC_WEBSOCKET_ACCEPT.toString() + "] header, however failed due to -> " + ex.getClass().getSimpleName() + " with message: " + ex.getMessage());
            }
        }
        if (request != null && request.headers().contains(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL)) {
            //tell the client we support everything he needs
            response.headers().set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, request.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL));
        }
        response.headers().set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, request);
        writeResponseWithKeepaliveHandling(ctx, response, request != null ? HttpHeaders.isKeepAlive(request) : false);
    }
    
    static void sendRedirect(ChannelHandlerContext ctx, String newUrl, Request.RequestHeader header) {
        logger.debug("Redirecting request to {}", newUrl);
        ByteBuf content = Unpooled.copiedBuffer("redirect to index.html", CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.TEMPORARY_REDIRECT, content);
        response.headers().set(HttpHeaderNames.LOCATION, newUrl);
        if (header != null && header.getMsgId() != null) {
            response.headers().set(AbstractMessage.PROP_CORRELATION_ID, header.getMsgId());
        }
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        writeResponseWithKeepaliveHandling(ctx, response, header != null ? header.isKeepalive() : false);
    }
    
}
