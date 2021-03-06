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
package org.thingsplode.synapse.serializers;

import org.thingsplode.synapse.core.MediaRange;
import org.thingsplode.synapse.core.MediaType;
import org.thingsplode.synapse.serializers.jackson.JacksonSerializer;

/**
 *
 * @author Csaba Tamas
 */
public class SerializationService {

    private static SerializationService instance = null;

    private final JacksonSerializer jacksonSerializer = new JacksonSerializer(true);

    private SerializationService() {
    }

    public static SerializationService getInstance() {
        if (instance == null) {
            synchronized (SerializationService.class) {
                instance = new SerializationService();
            }
        }
        return instance;
    }

    public SynapseSerializer<String> getPreferredSerializer(MediaRange mediaRange) {
        return jacksonSerializer;
    }

    public SynapseSerializer<String> getSerializer(MediaType contentType) {
        return jacksonSerializer;
    }
}
