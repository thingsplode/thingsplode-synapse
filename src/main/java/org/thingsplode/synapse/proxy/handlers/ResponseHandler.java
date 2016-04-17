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
package org.thingsplode.synapse.proxy.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.proxy.DispatcherFuture;
import org.thingsplode.synapse.proxy.DispatcherFutureHandler;

/**
 *
 * @author Csaba Tamas
 */
public class ResponseHandler extends SimpleChannelInboundHandler<Response> {
    
    private final DispatcherFutureHandler dfh;
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ResponseHandler.class);

    public ResponseHandler(DispatcherFutureHandler dfh) {
        this.dfh = dfh;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response msg) throws Exception {
        DispatcherFuture df =dfh.removeEntry(null);
        if (df == null) {
            logger.error("Response received but no request token was available. Discarding message.");
        } else {
            df.complete(msg);
        }
    }
    
 
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Excaption caught while expecting response: " + cause.getMessage());
        dfh.removeEntry(null).completeExceptionally(cause);
    }
    
}
