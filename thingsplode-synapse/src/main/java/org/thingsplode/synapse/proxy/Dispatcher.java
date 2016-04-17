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
package org.thingsplode.synapse.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.domain.AbstractMessage;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;

/**
 *
 * @author Csaba Tamas
 */
public class Dispatcher {

    private final Logger logger = LoggerFactory.getLogger(Dispatcher.class);
    private final Channel ch;
    private final DispatcherFutureHandler dispatcherFutureHandler;
    private final DispatcherPattern pattern;

    public Dispatcher(Channel ch, DispatcherFutureHandler dispatcherFutureHandler) {
        this.ch = ch;
        this.dispatcherFutureHandler = dispatcherFutureHandler;
        this.pattern = DispatcherPattern.CORRELATED_ASYNC;
    }

    public enum DispatcherPattern {

        /**
         * If this method is chosen, the client dispatcher will block until a
         * response is not received or a request timeout is not reached; It is
         * the least performing configuration, is only recommended for using the
         * Synapse client with 3rd party servers which do not support the
         * Pipelining or the message ID correlation.
         */
        BLOCKING_REQUEST,
        /**
         * The requests will be sent in a sequential order and the assumption is
         * made that the server supports pipelining (Eg. HTTP Pipelining /
         * sending the responses exactly in the same order as the requests were
         * received;
         * <b>Please be aware</b> that PIPELINING must be supported by the
         * server (The Synapse Endpoint Supports it).
         */
        PIPELINING,
        /**
         * The most efficient communication form: the client is asynchronously
         * dispatching the requests and the server returns the response in the
         * order of the execution (eg. Response for Request B may arrive earlier
         * that Response for Request A). In this case the server must support
         * the Id-Correlation ID paradigm (must set the ID of the request on the
         * Response Message Correlation-Id header property);
         * <b>Please be aware</b> that Message Correlation must be supported by
         * the server (The Synapse Endpoint Supports it).
         */
        CORRELATED_ASYNC;
    };

    /**
     * It will send a request in a fire&forget fashion, not expecting a
     * response;
     *
     * @param event
     * @return
     */
    public ChannelFuture broadcast(Request event) {
        return ch.writeAndFlush(event);
    }

    public DispatcherFuture<Request, Response> dispatch(Request request, long requestTimeout) throws InterruptedException {
        request.getHeader().setMsgId(UUID.randomUUID().toString());
        DispatcherFuture<Request, Response> dispatcherFuture = new DispatcherFuture<>(request, requestTimeout);
        if (pattern != null && (pattern == DispatcherPattern.CORRELATED_ASYNC || pattern == DispatcherPattern.BLOCKING_REQUEST)) {
            dispatcherFutureHandler.register(dispatcherFuture);
        }

        ChannelFuture cf = ch.writeAndFlush(request);
        cf.addListener((ChannelFutureListener) new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    //the message could not be sent
                    //todo: build a retry mechanism?
                    dispatcherFutureHandler.removeEntry((String) request.getRequestHeaderProperty(AbstractMessage.PROP_MESSAGE_ID).get());
                    dispatcherFuture.completeExceptionally(future.cause());
                    future.channel().close();
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Request message is succsfully dispatched: " + request.getHeader().getMsgId());
                }
            }
        });
        return dispatcherFuture;
    }

    public <T> T createStub(String servicePath, Class<T> aClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void destroy() throws InterruptedException {
        // Wait for the server to close the connection.
        ch.closeFuture().sync();
    }
}
