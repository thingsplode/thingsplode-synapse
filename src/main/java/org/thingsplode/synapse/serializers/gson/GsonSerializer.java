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
package org.thingsplode.synapse.serializers.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import org.thingsplode.synapse.core.MediaRange;
import org.thingsplode.synapse.core.exceptions.SerializationException;
import org.thingsplode.synapse.serializers.SynapseSerializer;
import org.thingsplode.synapse.serializers.gson.adapters.ClassTypeAdapter;
import org.thingsplode.synapse.serializers.gson.adapters.HttpResponseStatusAdapter;

/**
 *
 * @author Csaba Tamas
 */
public class GsonSerializer implements SynapseSerializer<String> {

    final Gson gson;
    private final MediaRange supportedRange = new MediaRange("application/json, text/json");

    public GsonSerializer(boolean prettyPrint, HashMap<Type, Object> typeaAdapters, List<ExclusionStrategy> exlusionStrategies) {
        GsonBuilder b = new GsonBuilder()
                .registerTypeAdapter(Class.class, new ClassTypeAdapter())
                .registerTypeAdapter(HttpResponseStatus.class, new HttpResponseStatusAdapter());
        if (typeaAdapters != null && !typeaAdapters.isEmpty()) {
            typeaAdapters.forEach((k, v) -> {
                b.registerTypeAdapter(k, v);
            });
        }
        if (exlusionStrategies != null && !exlusionStrategies.isEmpty()) {
            b.setExclusionStrategies((ExclusionStrategy[]) exlusionStrategies.toArray());
        }
        if (prettyPrint) {
            b.setPrettyPrinting();
        }
        this.gson = b.create();
    }

    @Override
    public String marshallToWireformat(Object src) throws SerializationException {
        try {
            //Type type = new TypeToken<Request<Device>>(){}.getType();
            return gson.toJson(src, src.getClass());
        } catch (Exception e) {
            throw new SerializationException("Couldn't marshall to json due to: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] marshall(Object src) throws SerializationException {
        if (src == null) {
            return new byte[0];
        }
        try {
            return gson.toJson(src, src.getClass()).getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new SerializationException("Could not serialize object of type [" + src.getClass().getName() + "] due to: " + ex.getMessage(), ex);
        }

    }

    @Override
    public <T> T unMarshall(Class<T> objectClass, String wirecontent) throws SerializationException {
        return gson.fromJson(wirecontent, objectClass);
    }

    @Override
    public MediaRange getSupportedMediaRange() {
        return supportedRange;
    }
}
