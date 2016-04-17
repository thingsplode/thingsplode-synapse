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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.exceptions.RequestTimeoutException;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
public class MsgIdRspCorrelator extends DispatcherFutureHandler {
    
    private final Logger logger = LoggerFactory.getLogger(MsgIdRspCorrelator.class);
    
    private ConcurrentHashMap<String, DispatcherFuture<Request, Response>> requestMsgRegistry;//msg id/msg correlation entry map
    
    public MsgIdRspCorrelator() {
        super();
        this.requestMsgRegistry = new ConcurrentHashMap<>();
        
    }

    /**
     * Called by the dispatch listener upon successful dispatch operation,
     * registers one request with its ID in order to be able to correlate a
     * future response to this request.
     *
     * @param msgEntry the request message itself (Request or Command)
     * @see DispatchListener
     */
    @Override
    public void register(DispatcherFuture<Request, Response> msgEntry) {
        if (msgEntry == null) {
            logger.warn(String.format("Did not registered Message Entry because is NULL"));
            return;
        }
        
        if (msgEntry.getRequest() == null || msgEntry.getRequest().getHeader() == null) {
            throw new IllegalArgumentException("At this stage the " + Request.class.getSimpleName() + " and its header should have been already set.");
        } else if (Util.isEmpty(msgEntry.getRequest().getHeader().getMsgId())) {
            throw new IllegalArgumentException("The " + MsgIdRspCorrelator.class.getSimpleName() + "requires a unique message id");
        }
        
        String msgId = msgEntry.getRequest().getHeader().getMsgId();
        
        if (msgId != null) {
            msgEntry.setRequestFiredTime(System.currentTimeMillis());
            DispatcherFuture oldMsgEntry = requestMsgRegistry.put(msgId, msgEntry);
            if (oldMsgEntry != null) {
                logger.warn(String.format("Duplicated registration for the same message with Message ID [%s] .\r\nOldMessage: %s\r\nNewMessage: %s", msgId, oldMsgEntry.toString(), msgEntry.toString()));
            }
        } else {
            logger.warn(String.format("Skipping the registration of a %s because the Message ID [%s]", MSG_ENTRY_NAME, msgId));
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug(debugStatuses());
        }
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
    @Override
    public DispatcherFuture<Request, Response> removeEntry(String msgId) {
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
     *
     * @return
     */
    public String debugStatuses() {
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
        return requestMapBuilder.toString();
        
    }

    @Override
    TimerTask getTimerTask() {
        return new SessionTimerTask();
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
                        DispatcherFuture msgWrapper = removeEntry(msgID);
                        msgWrapper.completeExceptionally(new RequestTimeoutException("Waiting for message has timed out [" + msgTimeout + "]ms in " + MsgIdRspCorrelator.class.getSimpleName()));
                    }
                });
            } catch (Throwable th) {
                logger.warn("There was an error in the correlation timer task execution: " + th.getMessage(), th);
            }
            
        }
    }
    
}
