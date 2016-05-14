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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.AbstractMessage;
import org.thingsplode.synapse.core.MediaType;
import org.thingsplode.synapse.proxy.EndpointProxy;

/**
 *
 * @author Csaba Tamas
 */
public class WSResponse2ResponseDecoder extends SimpleChannelInboundHandler<Object> {
    
    private final Logger logger = LoggerFactory.getLogger(WSResponse2ResponseDecoder.class);
    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private ScheduledFuture pingScheduledFuture;
    
    public WSResponse2ResponseDecoder(URI uri) {
        this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders());
    }
    
    public ChannelPromise getHandshakeFuture() {
        return handshakeFuture;
    }
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
        handshakeFuture.addListener((ChannelFutureListener) (ChannelFuture future) -> {
            if (future.isSuccess()) {
                //websocket HANDSHAKE was successfull, scheduing pinging
                pingScheduledFuture = ctx.channel().eventLoop().scheduleAtFixedRate(() -> {
                    ctx.writeAndFlush(new PingWebSocketFrame());
                }, 0, 60, TimeUnit.SECONDS);
            }
        });
        
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        pingScheduledFuture.cancel(true);
        logger.warn("WebSocket Client disconnected!");
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            logger.debug("Websocket@EndpointProxy: client is conencted.");
            handshakeFuture.setSuccess();
            return;
        }
        
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.getStatus()
                    + ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }
        
        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            AbstractMessage rcvdMsg = EndpointProxy.SERIALIZATION_SERVICE.getSerializer(MediaType.APPLICATION_JSON).unMarshall(AbstractMessage.class, textFrame.text());
            ctx.fireChannelRead(rcvdMsg);
        } else if (frame instanceof PongWebSocketFrame) {
            
        } else if (frame instanceof CloseWebSocketFrame) {
            logger.debug("Closing channel due to received: " + CloseWebSocketFrame.class.getSimpleName());
            ch.close();
            
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        logger.error("Unhandled exception caught at " + this.getClass().getSimpleName() + " with message: " + cause.getMessage(), cause);
        ctx.close();
    }
    
}
