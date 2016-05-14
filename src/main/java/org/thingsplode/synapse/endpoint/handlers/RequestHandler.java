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
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AttributeKey;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.AbstractMessage;
import org.thingsplode.synapse.core.ConnectionContext;
import org.thingsplode.synapse.core.FileRequest;
import org.thingsplode.synapse.core.MediaType;
import org.thingsplode.synapse.core.Request;
import org.thingsplode.synapse.core.Response;
import org.thingsplode.synapse.core.exceptions.MethodNotFoundException;
import org.thingsplode.synapse.core.exceptions.SynapseException;
import org.thingsplode.synapse.endpoint.ServiceRegistry;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
public class RequestHandler extends SimpleChannelInboundHandler<Request> {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    public static final AttributeKey<ConnectionContext> CONNECTION_CTX_ATTR = AttributeKey.valueOf("connection");
    private final ServiceRegistry registry;
    private final ChannelGroup channelRegistry;
    private final Pattern filePattern = Pattern.compile("\\/(.*)(.\\/)(.*)\\.[a-z]{3}");

    public RequestHandler(ServiceRegistry registry, ChannelGroup channelRegistry) {
        this.registry = registry;
        this.channelRegistry = channelRegistry;
        //ctx.channel().attr(ServerRouterHandler.CONNECTION_CTX_ATTR).get();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {
        Response response = null;
        boolean fileDownload = filePattern.matcher(request.getHeader().getUri().getPath()).find();
        if (!fileDownload) {
            try {
                if (request.getBody() != null && (request.getBody() instanceof ByteBuf)) {
                    //the body is in unmarshalled
                    //eg. http case
                    ByteBuf content = (ByteBuf) request.getBody();
                    byte[] dst = new byte[content.capacity()];
                    content.getBytes(0, dst);
                    String jsonBody = new String(dst, Charset.forName("UTF-8"));
                    response = registry.invokeWithParsable(request.getHeader(), jsonBody);
                } else {
                    //the complete body is unmarshalled already for an object
                    //eg. websocket case
                    response = registry.invokeWithObject(request.getHeader(), request.getBody());
                }
                //else {
                //    response = new Response(new Response.ResponseHeader(request.getHeader(), HttpResponseStatus.valueOf(HttpStatus.BAD_REQUEST.value()), new MediaType("text/plain; charset=UTF-8")), RequestHandler.class.getSimpleName() + ": Body type not supported.");
                //}
            } catch (MethodNotFoundException mex) {
                //simple listing, no stack trace (normal issue)
                logger.warn("Couldn't process request due to " + mex.getClass().getSimpleName() + " with message: " + mex.getMessage());
                response = new Response(new Response.ResponseHeader(request.getHeader(), HttpResponseStatus.valueOf(mex.getResponseStatus().value()), MediaType.TEXT_PLAIN), mex.getClass().getSimpleName() + ": " + mex.getMessage());
            } catch (SynapseException ex) {
                //it could be an internal issue
                logger.error("Error processing REST request: " + ex.getMessage(), ex);
                response = new Response(new Response.ResponseHeader(request.getHeader(), HttpResponseStatus.valueOf(ex.getResponseStatus().value()), MediaType.TEXT_PLAIN), ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }

        if (fileDownload || response == null || isFileDownloadRetriable(ctx, response)) {
            FileRequest fr = new FileRequest(request.getHeader());
            ctx.fireChannelRead(fr);
        } else {
            ctx.fireChannelRead(response);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //a client is connected
        channelRegistry.add(ctx.channel());
        ctx.channel().attr(CONNECTION_CTX_ATTR).set(new ConnectionContext(ctx));
    }

    private boolean isFileDownloadRetriable(ChannelHandlerContext ctx, Response response) {
        Optional<String> rcvChOpt = Util.getContextProperty(ctx, AbstractMessage.PROP_RCV_TRANSPORT);
        return response.getHeader().getResponseCode().equals(HttpResponseStatus.NOT_FOUND) && rcvChOpt.isPresent() && rcvChOpt.get().equalsIgnoreCase(AbstractMessage.PROP_HTTP_TRANSPORT);
    }

}
