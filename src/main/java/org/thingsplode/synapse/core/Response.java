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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Optional;
import org.thingsplode.synapse.serializers.jackson.adapters.HttpResponseStatusDeserializer;
import org.thingsplode.synapse.serializers.jackson.adapters.HttpResponseStatusSerializer;

/**
 *
 * @author Csaba Tamas
 * @param <T>
 */
@JsonPropertyOrder({ "@msg_type", "header", "body"})
public class Response<T> extends AbstractMessage<T> {

    private ResponseHeader header;

    public Response(ResponseHeader header) {
        super();
        this.header = header;
    }

    @JsonCreator
    public Response(@JsonProperty("header") ResponseHeader header, @JsonProperty("body") T body) {
        super(body);
        this.header = header;
    }

    @Override
    public ResponseHeader getHeader() {
        return header;
    }

    public void setHeader(ResponseHeader header) {
        this.header = header;
    }

    @Override
    public Optional<String> getHeaderProperty(String propertyKey) {
        return header != null ? header.getProperty(propertyKey) : Optional.empty();
    }

    @JsonPropertyOrder({ "protocolVersion", "contentType", "msgId", "correlationId","response_code","keepAlive","properties"})
    public static class ResponseHeader extends MessageHeader {

        @JsonSerialize(using = HttpResponseStatusSerializer.class)
        @JsonDeserialize(using = HttpResponseStatusDeserializer.class)
        @JsonProperty(value = "response_code")
        HttpResponseStatus responseCode = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        String correlationId;
        MediaType contentType;
        boolean keepAlive = false;

        public ResponseHeader(HttpResponseStatus responseCode) {
            this.responseCode = responseCode;
        }

        public ResponseHeader(Request.RequestHeader header, HttpResponseStatus responseCode) {
            this(responseCode);
            this.correlationId = header.getMsgId();
            this.keepAlive = header.isKeepalive();
        }

        public ResponseHeader(Request.RequestHeader header, HttpResponseStatus responseCode, MediaType media) {
            this(header, responseCode);
            this.contentType = media;
            this.keepAlive = header.isKeepalive();
        }

        @JsonCreator
        public ResponseHeader(@JsonProperty("msgId") String msgId, @JsonProperty("correlationId") String correlationId, @JsonProperty("response_code") HttpResponseStatus responseCode) {
            this.responseCode = responseCode;
            super.msgId = msgId;
            this.correlationId = correlationId;
        }

        public MediaType getContentType() {
            return contentType;
        }

        public void setContentType(MediaType contentType) {
            this.contentType = contentType;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public void setCorrelationId(String correlationId) {
            this.correlationId = correlationId;
        }

        public HttpResponseStatus getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(HttpResponseStatus responseCode) {
            this.responseCode = responseCode;
        }

        public boolean isKeepAlive() {
            return keepAlive;
        }

        public void setKeepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
        }

        @Override
        public String toString() {
            return "ResponseHeader{" + super.toString() + "responseCode=" + responseCode + ", correlationId=" + correlationId + ", keepAlive=" + keepAlive + '}';
        }
    }

    @Override
    public String toString() {
        return "Response{" + "header=" + header + super.toString() + '}';
    }

}
