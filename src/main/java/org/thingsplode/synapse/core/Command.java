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

import java.io.Serializable;

/**
 *
 * @author Csaba Tamas
 * @param <T>
 */
public class Command<T extends Serializable> extends AbstractMessage<T> {
    
    private CommandHeader header;

    public Command(CommandHeader header) {
        this.header = header;
    }

    public Command(CommandHeader header, T body) {
        super(body);
        this.header = header;
    }
    
    public CommandHeader getHeader() {
        return header;
    }

    public void setHeader(CommandHeader header) {
        this.header = header;
    }
    
    public static class CommandHeader extends MessageHeader {
        private long timeToLive;
        
        public long getTimeToLive() {
            return timeToLive;
        }

        public void setTimeToLive(long timeToLive) {
            this.timeToLive = timeToLive;
        }
        
        public CommandHeader(long timeToLive) {
            this.timeToLive = timeToLive;
        }
        
        public CommandHeader(long timeToLive, String msgId) {
            super(msgId);
            this.timeToLive = timeToLive;
        }
        
    }
}
