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
import org.thingsplode.synapse.core.domain.Event;
import org.thingsplode.synapse.endpoint.AbstractEventSink;

/**
 *
 * @author Csaba Tamas
 */
public class TestEventProcessor extends AbstractEventSink<Serializable> {

    public static ArrayBlockingQueue<Event<Serializable>> eventQueue = new ArrayBlockingQueue<>(1000);
    
    public TestEventProcessor() {
        super(Serializable.class);
    }

    @Override
    protected void eventReceived(Event<Serializable> event) {
        try {
            System.out.println("Event@TestEventProcessor at received: " + event.toString());
            TestEventProcessor.eventQueue.put(event);
        } catch (InterruptedException ex) {
            System.out.println("INTERRUPTED WHILE TRYING TO PUT IN THE EVENT QUEUE");
        }
    }
    
}
