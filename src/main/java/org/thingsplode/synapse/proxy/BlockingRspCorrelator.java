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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.domain.HttpStatus;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.exceptions.RequestTimeoutException;
import org.thingsplode.synapse.core.exceptions.SynapseException;

/**
 *
 * @author Csaba Tamas
 */
public class BlockingRspCorrelator implements DispatchedFutureHandler {

    private final Logger logger = LoggerFactory.getLogger(BlockingRspCorrelator.class);
    private final ArrayBlockingQueue<DispatchedFuture> requestQueue = new ArrayBlockingQueue(1);
    private TimerTask timerTask;
    private ScheduledFuture schedule;
    private long defaultResponseTimeout = finalDefaultTimeout;

    @Override
    public void beforeDispatch(DispatchedFuture<Request, Response> df) throws InterruptedException {
        //todo: prepare for message timeout and close connection upon timeout
        df.setRequestFiredTime(System.currentTimeMillis());
        long timeout = df.getTimeout() != -1 ? df.getTimeout() : defaultResponseTimeout;
        if (timeout > 0) {
            schedule = df.getChannel().eventLoop().schedule(getTimerTask(), timeout, TimeUnit.MILLISECONDS);

        }
        requestQueue.put(df);
    }

    @Override
    public DispatchedFuture<Request, Response> responseReceived(String msgId) {
        if (schedule != null) {
            schedule.cancel(false);
        }
        if (timerTask != null) {
            timerTask.cancel();
        }
        return requestQueue.poll();
    }

    @Override
    public TimerTask getTimerTask() {
        if (timerTask == null) {
            timerTask = new TimeoutTriggeringTimerTask();
        }
        return timerTask;
    }

    @Override
    public void setDefaultTimeout(Long responseTimeout) {
        this.defaultResponseTimeout = responseTimeout;
    }

    @Override
    public void evictActiveRequests() {
        DispatchedFuture f = requestQueue.poll();
        if (f != null){
            f.completeExceptionally(new SynapseException("Pending responses are evicted", HttpStatus.BAD_GATEWAY));
        }
    }

    public class TimeoutTriggeringTimerTask extends TimerTask {

        @Override
        public void run() {
            try {
                DispatchedFuture dispatchedMessage = requestQueue.poll();
                if (dispatchedMessage != null) {
                    dispatchedMessage.getChannel().disconnect().addListener((ChannelFutureListener) (ChannelFuture future) -> {
                        if (future.isSuccess()) {
                            logger.warn("Channel is closed as a consequence of a message timeout.");
                        }
                    }).await();
                    logger.trace("Generating Timeout Exception for a request message.");
                    dispatchedMessage.completeExceptionally(new RequestTimeoutException("Response for request message has timed out [" + dispatchedMessage.getTimeout() + "]ms in " + this.getClass().getSimpleName()));
                }
            } catch (Exception ex) {
                logger.error("Exception caught in timer -> " + ex.getMessage(), ex);
            }
        }
    }
}
