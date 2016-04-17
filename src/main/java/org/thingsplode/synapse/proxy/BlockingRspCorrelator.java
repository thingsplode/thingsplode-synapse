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

import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.exceptions.RequestTimeoutException;
import static org.thingsplode.synapse.proxy.DispatcherFutureHandler.MSG_ENTRY_NAME;

/**
 *
 * @author Csaba Tamas
 */
public class BlockingRspCorrelator extends DispatcherFutureHandler {

    private final Logger logger = LoggerFactory.getLogger(BlockingRspCorrelator.class);
    private final ArrayBlockingQueue<DispatcherFuture> requestQueue = new ArrayBlockingQueue(1);

    public BlockingRspCorrelator() {
        super();
    }

    @Override
    public void register(DispatcherFuture<Request, Response> df) throws InterruptedException {
        //todo: prepare for message timeout and close connection upon timeout
        requestQueue.put(df);
    }

    @Override
    public DispatcherFuture<Request, Response> removeEntry(String msgId) {
        return requestQueue.poll();
    }

    @Override
    TimerTask getTimerTask() {
        return new RequestTimeoutTimerTask();
    }

    public class RequestTimeoutTimerTask extends TimerTask {

        @Override
        public void run() {
            Long now = System.currentTimeMillis();
            DispatcherFuture df = requestQueue.peek();
            if (df == null) {
                return;
            }
            long msgTimeout = df.getTimeout() != -1 ? df.getTimeout() : defaultTimeout;
            long diffInSec = now - df.getRequestFiredTime();
            if (df.getTimeout() != 0 // 0 means that the timeout is ignored (infinite timeout)
                    && diffInSec > msgTimeout) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("Generating timeout for a %s because [now - fires = %s > %s]", MSG_ENTRY_NAME, diffInSec, msgTimeout));
                }
                DispatcherFuture msgWrapper = removeEntry(null);
                msgWrapper.completeExceptionally(new RequestTimeoutException("Waiting for message has timed out [" + msgTimeout + "]ms in " + this.getClass().getSimpleName()));
            }
        }
    }

}
