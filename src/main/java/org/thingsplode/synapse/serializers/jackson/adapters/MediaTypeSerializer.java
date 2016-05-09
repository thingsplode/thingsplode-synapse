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
package org.thingsplode.synapse.serializers.jackson.adapters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.thingsplode.synapse.core.MediaType;

/**
 *
 * @author Csaba Tamas
 */
public class MediaTypeSerializer extends StdSerializer<MediaType> {

    public MediaTypeSerializer() {
        super(MediaType.class);
    }

    @Override
    public void serialize(MediaType mediaType, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(mediaType.getName() != null ? mediaType.getName() : "");
    }
    
}
