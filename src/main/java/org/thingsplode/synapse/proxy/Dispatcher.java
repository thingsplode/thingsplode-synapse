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
import java.util.concurrent.CompletableFuture;
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
    private final ResponseCorrelatorService correlatorService;

    public Dispatcher(Channel ch, ResponseCorrelatorService correlatorService) {
        this.ch = ch;
        this.correlatorService = correlatorService;
    }

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

    public CompletableFuture<Response> dispatch(Request request, long requestTimeout) {
        request.getHeader().addMessageProperty(AbstractMessage.MESSAGE_ID, UUID.randomUUID().toString());
        DispatcherFuture<Request, Response> responseFuture = new DispatcherFuture<>(request, requestTimeout);
        ChannelFuture cf = ch.writeAndFlush(request);
        cf.addListener((ChannelFutureListener) new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    //the message could not be sent
                    //todo: build a retry mechanism?
                    responseFuture.completeExceptionally(future.cause());
                    future.channel().close();
                } else {
                    correlatorService.register(request.getHeader().getMessageProperty(AbstractMessage.MESSAGE_ID).get(), responseFuture);
                    if (logger.isDebugEnabled()){
                        correlatorService.debugStatuses();
                    }
                }
            }
        });
        return responseFuture;
    }

    public <T> T createStub(String servicePath, Class<T> aClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void destroy() throws InterruptedException {
        // Wait for the server to close the connection.
        ch.closeFuture().sync();
    }
}
