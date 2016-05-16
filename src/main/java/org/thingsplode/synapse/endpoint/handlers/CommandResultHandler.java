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
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.DispatchedFuture;
import org.thingsplode.synapse.MsgIdRspCorrelator;
import org.thingsplode.synapse.core.CommandResult;

/**
 *
 * @author Csaba Tamas
 */
public class CommandResultHandler extends SimpleChannelInboundHandler<CommandResult> {

    private final Logger logger = LoggerFactory.getLogger(CommandResultHandler.class);
    private MsgIdRspCorrelator resCorrelator;

    public CommandResultHandler(MsgIdRspCorrelator resultCorrelator) {
        if (resultCorrelator == null) {
            throw new IllegalArgumentException("The " + MsgIdRspCorrelator.class.getSimpleName() + "cannot be null.");
        }
        this.resCorrelator = resultCorrelator;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CommandResult msg) throws Exception {
        DispatchedFuture df = resCorrelator.responseReceived(msg.getHeader().getCorrelationId());
        if (df == null) {
            logger.error("Response received but no request token was available for correlation id: [" + msg.getHeader().getCorrelationId() + "]. Discarding message.");
        } else {
            try {
                df.complete(msg);
            } catch (Throwable th) {
                df.completeExceptionally(th);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(cause.getClass().getSimpleName() + "Exception caught while expecting response: " + cause.getMessage(), cause);
    }

}
