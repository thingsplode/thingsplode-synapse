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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;

/**
 *
 * @author Csaba Tamas
 */
public abstract class DispatcherFutureHandler {
    private final Logger logger = LoggerFactory.getLogger(DispatcherFutureHandler.class);
    protected final static String MSG_ENTRY_NAME = DispatcherFuture.class.getSimpleName();
    protected final static long SECOND = 1000L;
    protected final static long MINUTE = 60 * SECOND;
    protected Long defaultTimeout = 3 * MINUTE;
    private final ScheduledExecutorService executor;

    public DispatcherFutureHandler() {
        executor = Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
            Thread t = new Thread(r, "timer-thread");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((Thread t1, Throwable e) -> {
                logger.error("Ucaught exception while executing timer-task:" + e.getMessage(), e);
            });
            return t;
        });
        executor.scheduleAtFixedRate(getTimerTask(), 0, SECOND, TimeUnit.MILLISECONDS);
    }

    /**
     * The response timeout the period of time until the correlator will wait
     * for a Response, before generating a timeout message.
     * <br><br>
     * The response timeout shall be placed on each individual message entry
     * ({@link SendableMsgWrapper}), the default only provides a predefined
     * value in case somebody forgets to set a timeout value on a given message.
     * <br><br>
     * The default value is set to 3 minutes;
     *
     * @param responseTimeout
     */
    public void setDefaultTimeout(Long responseTimeout) {
        this.defaultTimeout = responseTimeout;
    }
    
    /**
     * Called by the dispatch listener upon successful dispatch operation;
     *
     * @param dispactherFuture the request message itself (Request or Command)
     * @throws java.lang.InterruptedException
     * @see DispatchListener
     */
    public abstract void register(DispatcherFuture<Request,Response> dispactherFuture) throws InterruptedException;
    
    public abstract DispatcherFuture<Request, Response> removeEntry(String msgId);
    
    abstract TimerTask getTimerTask();
}
