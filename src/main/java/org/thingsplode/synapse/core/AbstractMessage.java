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
package org.thingsplode.synapse.core;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import org.thingsplode.synapse.core.exceptions.MarshallerException;

/**
 *
 * @author Csaba Tamas
 * @param <T>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@msg_type", visible = true)
public abstract class AbstractMessage<T> implements Serializable {
    public final static String PROP_RCV_CHANNEL = "Receive-Channel";
    public final static String PROP_MESSAGE_ID = "Message-ID";
    public final static String PROP_CORRELATION_ID = "Correlation-ID";
    public final static String PROP_PROTOCOL_VERSION = "Protocol-Version";
    public final static String PROP_BODY_TYPE = "Body-Type";
    public final static String PROP_KEY_HTTP = "HTTP";
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@body_type", visible = true)
    private T body;

    public AbstractMessage() {
    }

    public AbstractMessage(T body) {
        this();
        this.body = body;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public <E> E expectBody(Class<E> type) throws MarshallerException {
        //todo: review if it makes sense
        if (body == null) {
            throw new MarshallerException("This message has not extension.", HttpStatus.EXPECTATION_FAILED);
        }
        try {
            return type.cast(body);
        } catch (ClassCastException ex) {
            throw new MarshallerException("The expected message is not type of " + type.getSimpleName() + ": " + ex.getMessage(), HttpStatus.EXPECTATION_FAILED, ex);
        }
    }

    @Override
    public String toString() {
        return "AbstractMessage{" + "body=" + body != null ? body.getClass().getSimpleName() : "null" + '}';
    }

}
