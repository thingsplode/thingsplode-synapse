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
package org.thingsplode.synapse.core.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Set;
import org.thingsplode.synapse.core.exceptions.MarshallerException;

/**
 *
 * @author tamas.csaba@gmail.com
 * @param <T>
 */
public abstract class AbstractMessage<T extends Serializable> implements Serializable {
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@body_type")
    private T body;

    public AbstractMessage() {
    }

    public AbstractMessage(T body) {
        this();
        this.body = body;
    }

    public static class MessageHeader {

        protected String msgId;
        private String protocolVersion;
        private InetSocketAddress callerAddress;
        private Set<Parameter> parameters;

        public MessageHeader() {
        }

        public MessageHeader(String msgId) {
            this.msgId = msgId;
        }

        public String getMsgId() {
            return msgId;
        }

        public void setMsgId(String msgId) {
            this.msgId = msgId;
        }

        public String getProtocolVersion() {
            return protocolVersion;
        }

        public void setProtocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
        }

        public InetSocketAddress getCallerAddress() {
            return callerAddress;
        }

        public void setCallerAddress(InetSocketAddress callerAddress) {
            this.callerAddress = callerAddress;
        }

        public Set<Parameter> getParameters() {
            return parameters;
        }

        public void setParameters(Set<Parameter> parameters) {
            this.parameters = parameters;
        }
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

}
