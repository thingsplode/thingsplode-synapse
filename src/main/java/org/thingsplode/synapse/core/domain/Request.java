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

import io.netty.handler.codec.http.HttpMethod;
import java.io.Serializable;

/**
 *
 * @author tamas.csaba@gmail.com
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

    public RequestHeader getHeader() {
        return header;
    }

    public void setHeader(RequestHeader header) {
        this.header = header;
    }

    public static class RequestHeader extends AbstractMessage.MessageHeader {

        private HttpMethod method;

        public RequestHeader(HttpMethod method) {
            this.method = method;
        }

        public HttpMethod getMethod() {
            return method;
        }

        public void setMethod(HttpMethod method) {
            this.method = method;
        }
    }

}
