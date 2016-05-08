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
package org.thingsplode.synapse.core;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author Csaba Tamas
 */
public class MessageHeader {

    protected String msgId;
    private String protocolVersion;
    private SocketAddress remoteAddress;
    private final HashMap<String, String> properties = new HashMap<>();

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

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(SocketAddress callerAddress) {
        this.remoteAddress = callerAddress;
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public Optional<String> getProperty(String propertyKey) {
        return Optional.ofNullable(properties.get(propertyKey));
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    public void addAllProperties(Iterable<Map.Entry<String, String>> iterable) {
        if (iterable != null) {
            iterable.forEach((e) -> properties.put(e.getKey(), e.getValue()));
        }
    }

}
