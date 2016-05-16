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
package org.thingsplode.synapse;

import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.PushNotification;
import org.thingsplode.synapse.proxy.AbstractNotificationSink;

/**
 *
 * @author Csaba Tamas
 */
public class TestNotificationSink extends AbstractNotificationSink {

    private final Logger logger = LoggerFactory.getLogger(TestNotificationSink.class);
    public static ArrayBlockingQueue<PushNotification<Serializable>> notificationQueue = new ArrayBlockingQueue<>(1000);

    @Override
    public void onMessage(PushNotification notification) {
        logger.debug("NOTIFICATION RECEIVED --> " + notification.toString());
        try {
            notificationQueue.put(notification);
        } catch (InterruptedException ex) {
            logger.error("EXCEPTION WHILE ADDING TO THE QUEUE --> " + ex.getMessage());
        }
    }
}
