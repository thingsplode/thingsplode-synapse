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
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;

/**
 *
 * @author Csaba Tamas
 */
public interface DispatchedFutureHandler {

    final static String MSG_ENTRY_NAME = DispatchedFuture.class.getSimpleName();
    final static long SECOND = 1000L;
    static long MINUTE = 60 * SECOND;

    /**
     *
     */
    Long finalDefaultTimeout = 3 * MINUTE;

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
    void setDefaultTimeout(Long responseTimeout);

    /**
     * Called by the dispatch listener upon successful dispatch operation;
     *
     * @param dispactherFuture the request message itself (Request or Command)
     * @throws java.lang.InterruptedException
     * @see DispatchListener
     */
    public abstract void beforeDispatch(DispatchedFuture<Request, Response> dispactherFuture) throws InterruptedException;

    public abstract DispatchedFuture<Request, Response> responseReceived(String msgId);

    abstract TimerTask getTimerTask();
    
    abstract void evictActiveRequests();
}
