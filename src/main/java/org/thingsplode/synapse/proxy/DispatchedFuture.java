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
package org.thingsplode.synapse.proxy;

import io.netty.channel.Channel;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author Csaba Tamas
 * @param <REQ>
 * @param <RSP>
 */
public class DispatchedFuture<REQ, RSP> extends CompletableFuture<RSP> {

    private final REQ request;
    private final Channel channel;
    private long timeout = 0;//a default request timeout which shall be overriden
    private long requestFiredTime = -1;//when was the message successfully dispatched

    /**
     *
     * @param req
     * @param ch
     * @param timeout in milliseconds
     */
    public DispatchedFuture(REQ req, Channel ch, long timeout) {
        this.request = req;
        this.timeout = timeout;
        this.channel = ch;
    }

    public Channel getChannel() {
        return channel;
    }

    /**
     * @return the timeout in milliseconds for receiving a response. If 0 means
     * unlimited;
     */
    public long getTimeout() {
        return timeout;
    }

    public REQ getRequest() {
        return request;
    }

    /**
     * @return the requestFiredTime is the moment when this request was
     * dispatched. It will return -1 if was not dispatched yet;
     */
    long getRequestFiredTime() {
        return requestFiredTime;
    }

    /**
     * @param requestFiredTime is the moment when this request was dispatched.
     */
    void setRequestFiredTime(long requestFiredTime) {
        this.requestFiredTime = requestFiredTime;
    }

}
