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
import io.netty.handler.codec.http.HttpMethod;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.thingsplode.synapse.core.domain.Request.RequestHeader;
import org.thingsplode.synapse.core.domain.Request.RequestHeader.RequestMethod;

/**
 *
 * @author Csaba Tamas
 * @param <T>
 */
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

    public RequestHeader getHeader() {
        return header;
    }

    public void setHeader(RequestHeader header) {
        this.header = header;
    }

    public Optional<String> getRequestHeaderProperty(String propertyKey) {
        return this.header != null ? this.header.getRequestProperty(propertyKey) : Optional.empty();
    }

    public static class RequestHeader extends AbstractMessage.MessageHeader {

        private Uri uri;
        private RequestMethod method;
        private final HashMap<String, String> requestProperties = new HashMap<>();

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

        public void addRequestProperty(String key, String value) {
            requestProperties.put(key, value);
        }

        public Optional<String> getRequestProperty(String propertyKey) {
            return Optional.ofNullable(requestProperties.get(propertyKey));
        }

        public HashMap<String, String> getRequestProperties() {
            return requestProperties;
        }

        public void addAllRequestProperties(Iterable<Map.Entry<String, String>> iterable) {
            if (iterable != null) {
                iterable.forEach(e -> requestProperties.put(e.getKey(), e.getValue()));
            }
        }

        public enum RequestMethod {

            /**
             * The GET method means retrieve whatever information (in the form
             * of an entity) is identified by the Request-URI. If the
             * Request-URI refers to a data-producing process, it is the
             * produced data which shall be returned as the entity in the
             * response and not the source text of the process, unless that text
             * happens to be the output of the process.
             */
            GET,
            /**
             * The POST method is used to request that the origin server accept
             * the entity enclosed in the request as a new subordinate of the
             * resource identified by the Request-URI in the Request-Line.
             */
            POST,
            /**
             * The PUT method requests that the enclosed entity be stored under
             * the supplied Request-URI.
             */
            PUT,
            /**
             * The DELETE method requests that the origin server delete the
             * resource identified by the Request-URI.
             */
            DELETE;

            public static RequestMethod fromHttpMethod(HttpMethod httpMethod) {
                return RequestMethod.valueOf(httpMethod.name());
            }
        }

    }
}
