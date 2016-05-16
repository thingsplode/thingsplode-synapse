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

/**
 *
 * @author Csaba Tamas
 */
@JsonPropertyOrder({"@msg_type", "header", "body"})
public class CommandResult extends AbstractMessage {

    private CommandResultHeader header;

    @JsonCreator
    public CommandResult(@JsonProperty("header") CommandResultHeader header) {
        this.header = header;
    }

    public CommandResult(CommandResultHeader header, Object body) {
        super(body);
        this.header = header;
    }

    public CommandResult(Command command, ResultCode resultCode) {
        this.header = new CommandResultHeader(resultCode, command.getHeader().getMsgId());
    }

    @Override
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

    @JsonPropertyOrder({"protocolVersion", "contentType", "msgId", "correlationId", "result", "errorReason", "properties"})
    public static class CommandResultHeader extends MessageHeader {

        private ResultCode result;
        private String correlationId;
        @JsonProperty("error")
        private String errorReason;
        MediaType contentType;

        @JsonCreator
        public CommandResultHeader(@JsonProperty("result") ResultCode result, @JsonProperty("correlation_id") String correlationId) {
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

        public String getErrorReason() {
            return errorReason;
        }

        public void setErrorReason(String errorReason) {
            this.errorReason = errorReason;
        }

        @Override
        public String toString() {
            return "CommandResultHeader{" + super.toString() + ", result=" + result + ", correlationId=" + correlationId + ", errorReason=" + errorReason + '}';
        }
    }

    @Override
    public String toString() {
        return "CommandResult{" + "header=" + header + super.toString() + '}';
    }

}
