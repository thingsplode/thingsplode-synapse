/*
 * Copyright 2016 Csaba Tamas.
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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;

/**
 * It is a wrapper around message bodies which are not packaged for some reason
 * in a {@link Request} or {@link Response} class (eg. when a {@link Response}
 * is converted to a {@link HttpResponse}, only the body is converted into Json
 * or other message format, while the original {@link Response} headers are
 * converted into {@link HttpHeaders}. In such cases a body object should be
 * transported via this wrapper, so that class information can also be
 * packaged).
 *
 * @author Csaba Tamas
 * @param <T>
 */
public class ResponseBodyWrapper<T> extends AbstractMessage<T> {

    @JsonCreator
    public ResponseBodyWrapper(@JsonProperty("body") T body) {
        super(body);
    }
}
