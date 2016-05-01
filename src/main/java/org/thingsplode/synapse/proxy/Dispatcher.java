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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
    private Channel channel;
    private final DispatchedFutureHandler dispatchedFutureHandler;
    private final DispatcherPattern pattern;
    private boolean retryConnection = false;
    private final AtomicInteger reconnectCounter = new AtomicInteger(0);
    private boolean destroying = false;
    private Bootstrap b;
    private String host;
    private int port;

    public Dispatcher(boolean retryConnection, DispatchedFutureHandler dispatchedFutureHandler, Bootstrap b, String host, int port) {
        this(dispatchedFutureHandler, b, host, port);
        this.retryConnection = retryConnection;
    }

    public Dispatcher(DispatchedFutureHandler dispatchedFutureHandler, Bootstrap b, String host, int port) {
        this.b = b;
        this.host = host;
        this.port = port;
        this.dispatchedFutureHandler = dispatchedFutureHandler;
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
        return channel.writeAndFlush(event);
    }

    public DispatchedFuture<Request, Response> dispatch(Request request, long requestTimeout) throws InterruptedException {
        if (!channel.isActive()) {
            if (!this.destroying) {
                logger.info("Channel is inactive. Trying to reconnect the proxy...");
                this.connect();
            } else {
                throw new IllegalStateException("The channel is closed and the dispatcher is a process of shutdown.");
            }
        }
        request.getHeader().setMsgId(UUID.randomUUID().toString());
        DispatchedFuture<Request, Response> dispatcherFuture = new DispatchedFuture<>(request, channel, requestTimeout);
        if (pattern != null && (pattern == DispatcherPattern.CORRELATED_ASYNC || pattern == DispatcherPattern.BLOCKING_REQUEST)) {
            dispatchedFutureHandler.beforeDispatch(dispatcherFuture);
        }

        ChannelFuture cf = channel.writeAndFlush(request);
        cf.addListener((ChannelFutureListener) new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    //the message could not be sent
                    //todo: build a retry mechanism?

                    dispatchedFutureHandler.responseReceived(request.getHeader().getMsgId());
                    dispatcherFuture.completeExceptionally(future.cause());
                    future.channel().close();
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Request message is succesfully dispatched with Msg. Id.: " + request.getHeader().getMsgId());
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
        logger.debug("Destroying " + Dispatcher.class.getSimpleName());
        this.setDestroying(true);
        channel.close().sync();
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isDestroying() {
        return destroying;
    }

    public void setDestroying(boolean destroying) {
        this.destroying = destroying;
    }

    protected void connect() throws InterruptedException {
        try {
            ChannelFuture cf = b.connect(host, port).addListener((ChannelFutureListener) (ChannelFuture future) -> {
                if (!future.isSuccess()) {
                    logger.warn("Connecting to the channel was not successfull.");
                } else {
                    logger.debug("Connected Channel@EndpointProxy");
                }
            }).sync();

            while (!cf.isSuccess()) {
                logger.warn("Connection attempt was not successfull / retrying...");
                cf = handleReconnect(b, host, port);
                if (cf == null) {
                    break;
                }
            }

            this.channel = cf.channel();
            if (channel == null) {
                throw new InterruptedException("Cannot connect.");
            }
            channel.closeFuture().addListener((ChannelFutureListener) (ChannelFuture future) -> {
                if (future.isSuccess()) {
                    logger.debug("Channel@Proxy is closed.");
                }
            });
        } catch (Exception ex) {
            logger.warn("Could not connect: " + ex.getMessage(), ex);
            ChannelFuture cf2 = handleReconnect(b, host, port);
            if (cf2 != null) {
                this.channel = cf2.channel();
            } else {
                throw ex;
            }
        }
    }

    private ChannelFuture handleReconnect(Bootstrap b, String host, int port) throws InterruptedException {
        if (retryConnection) {
            int currentReconnectCounter = reconnectCounter.incrementAndGet();
            if (currentReconnectCounter > 5) {
                //increasing the break between reconnects up to 5 seconds. Afterwards retrying every 5 second;
                currentReconnectCounter = 5;
            }
            Thread.sleep(currentReconnectCounter);
            return b.connect(host, port).sync();
        } else {
            return null;
        }
    }
}
