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
package org.thingsplode.synapse.serializers.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConfigFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.thingsplode.synapse.core.domain.MediaRange;
import org.thingsplode.synapse.core.exceptions.SerializationException;
import org.thingsplode.synapse.serializers.SynapseSerializer;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
public class JacksonSerializer implements SynapseSerializer<String> {

    private final ObjectMapper mapper;
    private SimpleModule module;
    private final MediaRange supportedRange = new MediaRange("application/json, text/json");

    private JacksonSerializer() {
        mapper = new ObjectMapper();
        module = new SimpleModule("CustomModule", new com.fasterxml.jackson.core.Version(1, 0, 0, null, null, null));
        //module.addDeserializer(ParameterWrapper.class, new ParameterWrapperDeserializer(ParameterWrapper.class));
        //mapper.registerModule(module);
    }

    public JacksonSerializer(boolean prettyPrint) {
        this();
        List<ConfigFeature> allowedFeatures = new ArrayList<>();
        List<ConfigFeature> disabledFeatures = new ArrayList<>();

        disabledFeatures.add(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        disabledFeatures.add(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        disabledFeatures.add(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        allowedFeatures.add(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //allowedFeatures.add(SerializationFeature.WRAP_ROOT_VALUE);
        allowedFeatures.add(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        mapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        init(prettyPrint, allowedFeatures, disabledFeatures, PropertyNamingStrategy.SNAKE_CASE);
    }

    public JacksonSerializer(boolean prettyPrint, List<ConfigFeature> allowFeatures, List<ConfigFeature> disableFeatures, PropertyNamingStrategy namingStrategy) {
        this();
        init(prettyPrint, allowFeatures, disableFeatures, namingStrategy);
    }

    @Override
    public MediaRange getSupportedMediaRange() {
        return supportedRange;
    }

    @Override
    public String marshallToWireformat(Object object) throws SerializationException {
        try {
            if (object != null) {
                return mapper.writeValueAsString(object);
            } else {
                return "";
            }
        } catch (JsonProcessingException ex) {
            throw new SerializationException("Could not serialize object of type [" + object.getClass().getSimpleName() + "] due to: " + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] marshall(Object object) throws SerializationException {
        if (object == null) {
            return new byte[0];
        }
        try {
            return mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException ex) {
            throw new SerializationException("Could not serialize object of type [" + object.getClass().getName() + "] due to: " + ex.getMessage(), ex);
        }
    }

    @Override
    public <T> T unMarshall(Class<T> objectType, String wirecontent) throws SerializationException {
        try {
            if (!Util.isEmpty(wirecontent)) {
                return mapper.readValue(wirecontent, objectType);
            } else {
                return null;
            }
        } catch (IOException ex) {
            throw new SerializationException("Could not deserialize content: " + ex.getMessage(), ex);
        }
    }

    private void init(boolean prettyPrint, List<ConfigFeature> allowedFeatures, List<ConfigFeature> disabledFeatures, PropertyNamingStrategy propNaming) {
        //mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
        //mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        if (prettyPrint) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }

        mapper.setPropertyNamingStrategy(propNaming);

        if (allowedFeatures != null && !allowedFeatures.isEmpty()) {
            allowedFeatures.forEach(f -> configure(f, Switch.ENABLE));
        }
        if (disabledFeatures != null && !disabledFeatures.isEmpty()) {
            disabledFeatures.forEach(f -> configure(f, Switch.DISABLE));
        }

        //registerHandlers(serializers, deSerializers);
    }

    public void addSerializer(JsonSerializer serializer) {
        module.addSerializer(serializer);
    }

    public void addDeserializer(Class clazz, JsonDeserializer deserializer) {
        module.addDeserializer(clazz, deserializer);
    }

    private void registerHandlers(List<JsonSerializer> serializers, HashMap<Class, JsonDeserializer> deSerializers) {

        if (serializers != null && !serializers.isEmpty()) {
            serializers.forEach(s -> module.addSerializer(s));
        }
        if (deSerializers != null && !deSerializers.isEmpty()) {
            deSerializers.forEach((k, v) -> module.addDeserializer(k, v));
        }

    }

    private void configure(ConfigFeature f, Switch sw) {
        switch (sw) {
            case ENABLE: {
                if (f instanceof DeserializationFeature) {
                    mapper.enable((DeserializationFeature) f);
                } else if (f instanceof MapperFeature) {
                    mapper.enable((MapperFeature) f);
                } else if (f instanceof SerializationFeature) {
                    mapper.enable((SerializationFeature) f);
                }
                break;
            }
            case DISABLE: {
                if (f instanceof DeserializationFeature) {
                    mapper.disable((DeserializationFeature) f);
                } else if (f instanceof MapperFeature) {
                    mapper.disable((MapperFeature) f);
                } else if (f instanceof SerializationFeature) {
                    mapper.disable((SerializationFeature) f);
                }
                break;
            }
        }
    }

    private enum Switch {
        ENABLE,
        DISABLE
    }
}
