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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.thingsplode.synapse.serializers.jackson.adapters.HttpResponseStatusDeserializer;
import org.thingsplode.synapse.serializers.jackson.adapters.HttpResponseStatusSerializer;

/**
 *
 * @author Csaba Tamas
 * @param <T>
 */
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

    public ResponseHeader getHeader() {
        return header;
    }

    public void setHeader(ResponseHeader header) {
        this.header = header;
    }

    public static class ResponseHeader extends AbstractMessage.MessageHeader {

        @JsonSerialize(using = HttpResponseStatusSerializer.class)
        @JsonDeserialize(using = HttpResponseStatusDeserializer.class)
        private HttpResponseStatus responseCode = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        private String correlationId;
        private MediaType contentType;

        public ResponseHeader(HttpResponseStatus responseCode) {
            this.responseCode = responseCode;
        }

        public ResponseHeader(Request.RequestHeader header, HttpResponseStatus responseCode) {
            this(responseCode);
            this.correlationId = header.getMsgId();
        }

        public ResponseHeader(Request.RequestHeader header, HttpResponseStatus responseCode, MediaType media) {
            this(header, responseCode);
            this.contentType = media;
        }

        @JsonCreator
        public ResponseHeader(@JsonProperty("msgId") String msgId, @JsonProperty("correlationId") String correlationId, @JsonProperty("responseCode") HttpResponseStatus responseCode) {
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

        public HttpResponseStatus getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(HttpResponseStatus responseCode) {
            this.responseCode = responseCode;
        }

    }

}
