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
package org.thingsplode.synapse.endpoint;

import java.io.Serializable;
import org.thingsplode.synapse.core.Event;

/**
 *
 * @author Csaba Tamas
 * @param <T>
 */
//todo: build common event handling logic
public abstract class AbstractEventSink<T extends Serializable> implements EventSink<T> {

    private final Class<T> clazz;

    public AbstractEventSink(Class<T> clazz) {
        this.clazz = clazz;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    @Override
    public void consume(Event<T> event) {
        eventReceived(event);
    }

    protected abstract void eventReceived(Event<T> event);

}
