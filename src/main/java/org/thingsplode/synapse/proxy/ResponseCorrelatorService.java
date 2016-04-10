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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.exceptions.RequestTimeoutException;

/**
 *
 * @author Csaba Tamas
 */
public class ResponseCorrelatorService {

    private final Logger logger = LoggerFactory.getLogger(ResponseCorrelatorService.class);
    private final static String MSG_ENTRY_NAME = DispatcherFuture.class.getSimpleName();
    private final static long SECOND = 1000L;
    private final static long MINUTE = 60 * SECOND;
    private Long defaultTimeout = 3 * MINUTE;
    private ConcurrentHashMap<String, DispatcherFuture<Request, Response>> requestMsgRegistry;//msg id/msg correlation entry map
    private final ScheduledExecutorService executor;

    public ResponseCorrelatorService() {
        this.requestMsgRegistry = new ConcurrentHashMap<>();
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new SessionTimerTask(), 0, SECOND, TimeUnit.MILLISECONDS);
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
     * Called by the dispatch listener upon successful dispatch operation,
     * registers one request with its ID in order to be able to correlate a
     * future response to this request.
     *
     * @param msgId the ID which is used to uniquely identify this request
     * @param msgEntry the request message itself (Request or Command)
     * @return the previous value associated with key, or null if there was no
     * mapping for key. (A null return can also indicate that the map previously
     * associated null with key, if the implementation supports null values.)
     * @see DispatchListener
     */
    DispatcherFuture<Request, Response> register(String msgId, DispatcherFuture msgEntry) {
        if (msgEntry == null) {
            logger.warn(String.format("Did not registered Message ID [%s] because msgEntry is NULL", msgId));
            return null;
        }
        if (msgId != null) {
            msgEntry.setRequestFiredTime(System.currentTimeMillis());
            DispatcherFuture oldMsgEntry = requestMsgRegistry.put(msgId, msgEntry);
            if (oldMsgEntry != null) {
                logger.warn(String.format("Duplicated registration for the same message with Message ID [%s] .\r\nOldMessage: %s\r\nNewMessage: %s", msgId, oldMsgEntry.toString(), msgEntry.toString()));
            }
            return oldMsgEntry;
        } else {
            logger.warn(String.format("Skipping the registration of a %s because the Message ID [%s]", MSG_ENTRY_NAME, msgId));
        }
        return null;
    }

    /**
     * Removes one request based on the message ID/correlation ID
     *
     * @param msgId the message ID of the request (or the correlation ID of the
     * response)
     * @return the previous value associated with key, or null if there was no
     * mapping for key.
     * @see ReceiverListener
     */
    DispatcherFuture<Request, Response> removeMsgEntry(String msgId) {
        if (msgId != null) {
            DispatcherFuture deleted = requestMsgRegistry.remove(msgId);
            if (deleted == null) {
                logger.warn(String.format("Removal of %s with message ID [%s] failed, because it was not inlcuded in the reponse correlation registry anymore.", MSG_ENTRY_NAME, msgId));
            }
            return deleted;
        } else {
            return null;
        }
    }

    /**
     * A convenience debug method to list the content of request registry.
     */
    public void debugStatuses() {
        /**
         * This part is called only of there's already trouble. The debug
         * information is very much needed in order to be able to reproduce the
         * problems.
         */
        StringBuilder requestMapBuilder = new StringBuilder("\n***************************\nREQUEST MAP's ACTUAL STATUS: \n");
        requestMsgRegistry.keySet().stream().forEach((corrId) -> {
            requestMapBuilder.append("MSGID: ").append(corrId).append("Callback Listener: ").append(requestMsgRegistry.get(corrId)).append("\n");
        });
        requestMapBuilder.append("**** END OF SESSION REGISTRY ****");
        logger.debug(requestMapBuilder.toString());

    }

    class SessionTimerTask extends TimerTask {

        @Override
        public void run() {
            try {
                Long now = System.currentTimeMillis();
                requestMsgRegistry.keySet().stream().forEach((msgID) -> {
                    DispatcherFuture msgEntry = requestMsgRegistry.get(msgID);
                    long msgTimeout = msgEntry.getTimeout() != -1 ? msgEntry.getTimeout() : defaultTimeout;
                    long diffInSec = now - msgEntry.getRequestFiredTime();
                    if (msgEntry.getTimeout() != 0 // 0 means that the timeout is ignored (infinite timeout)
                            && diffInSec > msgTimeout) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("Generating timeout for a %s with message ID [%s] because [now - fires = %s > %s]", MSG_ENTRY_NAME, msgID, diffInSec, msgTimeout));
                        }
                        DispatcherFuture msgWrapper = removeMsgEntry(msgID);
                        msgWrapper.completeExceptionally(new RequestTimeoutException("Waiting for message has timed out [" + msgTimeout + "] in " + ResponseCorrelatorService.class.getSimpleName()));
                    }
                });
            } catch (Throwable th) {
                logger.warn("There was an error in the correlation timer task execution: " + th.getMessage(), th);
            }

        }
    }

}
