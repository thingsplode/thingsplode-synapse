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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.Response;
import org.thingsplode.synapse.DispatchedFuture;
import org.thingsplode.synapse.DispatchedFutureHandler;

/**
 *
 * @author Csaba Tamas
 */
@ChannelHandler.Sharable
public class ResponseHandler extends SimpleChannelInboundHandler<Response> {

    private final DispatchedFutureHandler dfh;
    private static final Logger logger = LoggerFactory.getLogger(ResponseHandler.class);

    public ResponseHandler(DispatchedFutureHandler dfh) {
        if (dfh == null) {
            throw new IllegalArgumentException("The " + DispatchedFutureHandler.class.getSimpleName() + "cannot be null.");
        }
        this.dfh = dfh;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response msg) throws Exception {
        DispatchedFuture df = dfh.responseReceived(msg.getHeader().getCorrelationId());
        if (df == null) {
            logger.error("Response received but no request token was available for correlation id: [" + msg.getHeader().getCorrelationId() + "]. Discarding message.");
        } else {
            df.complete(msg);
        }
        ///ctx.
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (logger.isDebugEnabled()) {
            logger.error(cause.getClass().getSimpleName() + "Exception caught while expecting response: " + cause.getMessage(), cause);
        }
        DispatchedFuture df = dfh.responseReceived(null);
        if (df != null) {
            df.completeExceptionally(cause);
        }

    }

}
