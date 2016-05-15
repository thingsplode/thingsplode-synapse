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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 *
 * @author Csaba Tamas
 * @param <T>
 */
public class PushNotification<T extends Serializable> extends Command<T> {

    @JsonCreator
    public PushNotification(@JsonProperty("header") NotificationHeader header) {
        super(header);
    }

    public static class NotificationHeader extends Command.CommandHeader {

        private String topic;

        @JsonCreator
        public NotificationHeader(@JsonProperty("time_to_live") long timeToLive) {
            super(timeToLive);
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }

    public void setHeader(NotificationHeader header) {
        super.setHeader(header);
    }

    @Override
    public NotificationHeader getHeader() {
        return (NotificationHeader) super.getHeader();
    }

}
