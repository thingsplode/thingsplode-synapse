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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.thingsplode.synapse.core.MediaType;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
public class MediaTypeDeserializer extends StdDeserializer<MediaType> {
    
    public MediaTypeDeserializer() {
        super(MediaType.class);
    }
    
    @Override
    public MediaType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.readValueAsTree();
        JsonNode mTypeNode = node.get("media_type");
        if (mTypeNode == null) {
            mTypeNode = node.get("content_type");
        }
        if (mTypeNode == null || Util.isEmpty(mTypeNode.asText())) {
            return null;
        } else {
            return new MediaType(mTypeNode.asText());
        }
    }
}
