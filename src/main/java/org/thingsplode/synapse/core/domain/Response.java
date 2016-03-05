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
import java.io.Serializable;
import org.thingsplode.synapse.endpoint.serializers.jackson.adapters.HttpResponseStatusDeserializer;
import org.thingsplode.synapse.endpoint.serializers.jackson.adapters.HttpResponseStatusSerializer;

/**
 *
 * @author tamas.csaba@gmail.com
 * @param <T>
 */
public class Response<T extends Serializable> extends AbstractMessage<T> {

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

        
        public ResponseHeader(HttpResponseStatus responseCode) {
            this.responseCode = responseCode;
        }

        @JsonCreator
        public ResponseHeader(@JsonProperty("msgId") String msgId, @JsonProperty("correlationId") String correlationId, @JsonProperty("responseCode") HttpResponseStatus responseCode) {
            this(responseCode);
            super.msgId = msgId;
            this.correlationId = correlationId;
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
