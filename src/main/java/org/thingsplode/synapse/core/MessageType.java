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

/**
 *
 * @author Csaba Tamas
 */
public enum MessageType {
    REQUEST((byte) 1, Request.class),
    RESPONSE((byte) 2, Response.class),
    EVENT((byte) 3, Event.class),
    COMMAND((byte) 4, Command.class),
    COMMAND_RESULT((byte) 5, CommandResult.class),
    PUSH_NOTIFICATION((byte) 6, PushNotification.class);

    private final byte marker;
    private final Class messageClass;

    public static MessageType fromMarker(byte byteMarker) {
        for (MessageType mt : MessageType.values()) {
            if (byteMarker == mt.marker) {
                return mt;
            }
        }
        return null;
    }

    public static MessageType fromMessageClass(Class clazz) {
        for (MessageType mt : MessageType.values()) {
            if (clazz == mt.messageClass) {
                return mt;
            }
        }
        return null;
    }

    private MessageType(byte marker, Class messagClass) {
        this.marker = marker;
        this.messageClass = messagClass;
    }

    public Class getMessageClass() {
        return messageClass;
    }

    public byte getMarker() {
        return marker;
    }
}
