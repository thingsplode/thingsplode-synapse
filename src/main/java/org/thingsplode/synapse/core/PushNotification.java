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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 * @param <T>
 */
@JsonPropertyOrder({ "@msg_type", "header", "body"})
public class PushNotification<T extends Serializable> extends Command<T> {

    public final static String DEFAULT_NOTIFICATION_TOPIC = "/default";

    @JsonCreator
    public PushNotification(@JsonProperty("header") NotificationHeader header) {
        super(header);
    }

    public void setDefaultTopicIfNone() {
        if (super.getHeader() == null) {
            this.setHeader(new NotificationHeader(0));
        }
        if (Util.isEmpty(this.getHeader().getSourceTopic())) {
            this.getHeader().setSourceTopic(DEFAULT_NOTIFICATION_TOPIC);
        }
    }

    @JsonPropertyOrder({"protocolVersion", "msgId", "src_topic", "properties"})
    public static class NotificationHeader extends Command.CommandHeader {

        @JsonProperty("src_topic")
        private String sourceTopic;

        @JsonCreator
        public NotificationHeader(@JsonProperty("time_to_live") long timeToLive) {
            super(timeToLive);
        }

        public String getSourceTopic() {
            return sourceTopic;
        }

        public void setSourceTopic(String topic) {
            this.sourceTopic = topic;
        }

        @Override
        public String toString() {
            return "NotificationHeader{" + "sourceTopic=" + sourceTopic + super.toString() + '}';
        }
    }

    public void setHeader(NotificationHeader header) {
        super.setHeader(header);
    }

    @Override
    public NotificationHeader getHeader() {
        return (NotificationHeader) super.getHeader();
    }

    @Override
    public String toString() {
        return "PushNotification{" + super.toString() + '}';
    }
    
}
