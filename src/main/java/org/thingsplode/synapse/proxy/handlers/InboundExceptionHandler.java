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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.thingsplode.synapse.proxy.DispatcherFutureHandler;

/**
 *
 * @author Csaba Tamas
 */
public class InboundExceptionHandler extends ChannelInboundHandlerAdapter { //extends ChannelDuplexHandler {

    private final DispatcherFutureHandler dfh;
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(InboundExceptionHandler.class);

    public InboundExceptionHandler(DispatcherFutureHandler dispatcherFutureHandler) {
        this.dfh = dispatcherFutureHandler;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // Uncaught exceptions from inbound handlers will propagate up to this handler 
        logger.error("Exception caught on inbound pipe: " + cause.getMessage(), cause);
        dfh.removeEntry(null).completeExceptionally(cause);
    }

//    @Override
//    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise future) throws Exception {
//        ctx.connect(remoteAddress, localAddress, future.addListener((ChannelFutureListener) (ChannelFuture future1) -> {
//            if (!future1.isSuccess()) {
//                // Handle connect exception here...
//            }
//        }));
//    }
//
//    @Override
//    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
//        ctx.write(msg, promise.addListener((ChannelFutureListener) (ChannelFuture future) -> {
//            if (!future.isSuccess()) {
//                // Handle write exception here...
//            }
//        }));
//    }
    // ... override more outbound methods to handle their exceptions as well
}
