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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.thingsplode.synapse.core.exceptions.MarshallerException;

/**
 *
 * @author Csaba Tamas
 * @param <T>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@msg_type", visible = true)
public abstract class AbstractMessage<T> implements Serializable {

    /**
     * Describes the transport on which the current requests was received. The
     * possible transports are HTTP, WEB SOCKET, etc.
     */
    public final static String PROP_RCV_TRANSPORT = "Receive-Channel";

    /**
     * The unique message ID property which will be placed in the response's
     * correlation ID field;
     */
    public final static String PROP_MESSAGE_ID = "Message-ID";

    /**
     * The correlation ID field is used by response messages to tell the client
     * which request is answered by the current response;
     */
    public final static String PROP_CORRELATION_ID = "Correlation-ID";

    /**
     * The protocol version may help the unmarshalling processes to choose the
     * right strategy;
     */
    public final static String PROP_PROTOCOL_VERSION = "Protocol-Version";

    /**
     * Describes the body object type (usually full classpath);
     */
    public final static String PROP_BODY_TYPE = "Body-Type";

    /**
     * One possible value for the {@link PROP_RCV_TRANSPORT}, representing the
     * HTTP channel;
     */
    public final static String PROP_HTTP_TRANSPORT = "HTTP";

    /**
     * One possible value for the {@link PROP_RCV_TRANSPORT}, representing the
     * Web Socket channel;
     */
    public final static String PROP_WS_TRANSPORT = "";

    /**
     * A unique value, which is used to identify a connecting client;
     */
    public final static String PROP_CLIENT_ID = "Client-ID";
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

    public void addAllProperties(Iterable<Map.Entry<String, String>> iterable) {
        if (this.getHeader() != null) {
            this.getHeader().addAllProperties(iterable);
        }
    }

    @JsonIgnore
    public HashMap<String, String> getHeaderProperties() {
        //returning null instead of empty optional when there's no header, because a missing header should be found during early in the development phase
        return this.getHeader() != null ? this.getHeader().getProperties() : null;
    }

    @JsonIgnore
    public Optional<String> getHeaderProperty(String propertyKey) {
        //returning null instead of empty optional when there's no header, because a missing header should be found during early in the development phase
        return this.getHeader() != null ? this.getHeader().getProperty(propertyKey) : null;
    }

    public void addProperty(String key, String value) {
        if (this.getHeader() != null) {
            this.getHeader().addProperty(key, value);
        }
    }

    public abstract MessageHeader getHeader();

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
        return "body=" + (body != null ? body.getClass().getSimpleName() : "null");
    }

}
