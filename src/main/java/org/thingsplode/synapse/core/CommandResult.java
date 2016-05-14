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

/**
 *
 * @author Csaba Tamas
 */
public class CommandResult extends AbstractMessage {

    private CommandResultHeader header;

    public CommandResult(CommandResultHeader header) {
        this.header = header;
    }

    public CommandResult(CommandResultHeader header, Object body) {
        super(body);
        this.header = header;
    }

    public CommandResultHeader getHeader() {
        return header;
    }

    public void setHeader(CommandResultHeader header) {
        this.header = header;
    }

    public enum ResultCode {
        REJECTED,
        FAILED,
        EXECUTED,
        POSTED;
    }

    public static class CommandResultHeader extends MessageHeader {

        private ResultCode result;
        private String correlationId;
        MediaType contentType;

        public CommandResultHeader(ResultCode result, String correlationId) {
            this.result = result;
            this.correlationId = correlationId;
        }

        public CommandResultHeader(ResultCode result, String correlationId, MediaType contentType, String msgId) {
            super(msgId);
            this.result = result;
            this.correlationId = correlationId;
            this.contentType = contentType;
        }

        public ResultCode getResult() {
            return result;
        }

        public void setResult(ResultCode result) {
            this.result = result;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public void setCorrelationId(String correlationId) {
            this.correlationId = correlationId;
        }

        public MediaType getContentType() {
            return contentType;
        }

        public void setContentType(MediaType contentType) {
            this.contentType = contentType;
        }

    }

}
