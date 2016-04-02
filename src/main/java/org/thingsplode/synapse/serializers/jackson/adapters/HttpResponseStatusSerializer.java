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
package org.thingsplode.synapse.serializers.jackson.adapters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;

/**
 *
 * @author Csaba Tamas
 */
public class HttpResponseStatusSerializer extends StdSerializer<HttpResponseStatus> {
    
    public HttpResponseStatusSerializer() {
        super(HttpResponseStatus.class);
    }
    
    @Override
    public void serialize(HttpResponseStatus value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("code", value.code());
        gen.writeStringField("reason", value.reasonPhrase());
        gen.writeEndObject();
    }
    
}
