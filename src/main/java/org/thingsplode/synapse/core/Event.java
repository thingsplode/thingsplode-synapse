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

/**
 *
 * @author Csaba Tamas
 * @param <T>
 */
@JsonPropertyOrder({ "@msg_type", "header", "body"})
public class Event<T extends Serializable> extends Request<T> {

    @JsonCreator
    public Event(@JsonProperty("header") RequestHeader header) {
        super(header);
    }

    public Event(RequestHeader header, T body) {
        super(header, body);
    }

    public static Event create(String uri) throws UnsupportedEncodingException {
        return new Event(new RequestHeader(new Uri(uri), RequestMethod.POST));
    }

    public static Event create(String uri, Serializable body) throws UnsupportedEncodingException {
        return new Event(new RequestHeader(new Uri(uri), RequestMethod.POST), body);
    }

    @Override
    public String toString() {
        return "Event{" + super.toString() + '}';
    }
}
