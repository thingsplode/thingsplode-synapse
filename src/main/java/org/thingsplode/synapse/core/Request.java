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
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import org.thingsplode.synapse.core.Request.RequestHeader;

/**
 *
 * @author Csaba Tamas
 * @param <T>
 */
@JsonPropertyOrder({ "@msg_type", "header", "body"})
public class Request<T extends Serializable> extends AbstractMessage<T> {

    private RequestHeader header;

    public Request() {
    }

    public Request(RequestHeader header) {
        super();
        this.header = header;
    }

    public Request(RequestHeader header, T body) {
        super(body);
        this.header = header;
    }

    public static Request<?> create(String msgId, Uri uri, RequestMethod method, Serializable body) {
        return new Request(new RequestHeader(msgId, uri, method), body);
    }

    public static Request<?> create(String msgId, Uri uri, RequestMethod method) {
        return new Request(new RequestHeader(msgId, uri, method));
    }

    public static Request<?> create(Uri uri, RequestMethod method) {
        return new Request(new RequestHeader(uri, method));
    }

    public static Request<?> create(String uri, RequestMethod method) throws UnsupportedEncodingException {
        return new Request(new RequestHeader(new Uri(uri), method));
    }

    public static Request<?> create(String uri, RequestMethod method, Serializable body) throws UnsupportedEncodingException {
        return new Request(new RequestHeader(new Uri(uri), method), body);
    }

    public RequestHeader getHeader() {
        return header;
    }

    public void setHeader(RequestHeader header) {
        this.header = header;
    }

    public Optional<String> getRequestHeaderProperty(String propertyKey) {
        return this.header != null ? this.header.getProperty(propertyKey) : Optional.empty();
    }

    @Override
    public String toString() {
        return "Request{" + "header=" + header + super.toString() + '}';
    }

    public static class RequestHeader extends MessageHeader {

        /**
         * Used only internally at the Endpoint to keep track of
         * Request/Response message for message pipelining;
         */
        public static final String MSG_SEQ = "MSG_SEQ";
        Uri uri;
        RequestMethod method;
        boolean keepalive = false;

        public RequestHeader(Uri uri, RequestMethod method) {
            this(null, uri, method);
        }

        @JsonCreator
        public RequestHeader(@JsonProperty("msgId") String msgId, @JsonProperty("uri") Uri uri, @JsonProperty("method") RequestMethod method) {
            super(msgId);
            this.uri = uri;
            this.method = method;
        }

        public Uri getUri() {
            return uri;
        }

        public void setUri(Uri uri) {
            this.uri = uri;
        }

        public void setMethod(RequestMethod method) {
            this.method = method;
        }

        public RequestMethod getMethod() {
            return method;
        }

        public boolean isKeepalive() {
            return keepalive;
        }

        public void setKeepalive(boolean keepalive) {
            this.keepalive = keepalive;
        }

        @Override
        public String toString() {
            return "RequestHeader{" + "uri=" + uri + ", method=" + method + '}';
        }
    }
}
