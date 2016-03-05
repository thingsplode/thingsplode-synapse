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
package org.thingsplode.synapse.endpoint.serializers;

import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import org.thingsplode.synapse.core.domain.MediaRange;
import org.thingsplode.synapse.core.exceptions.SerializationException;
import org.thingsplode.synapse.endpoint.serializers.adapters.ClassTypeAdapter;
import org.thingsplode.synapse.endpoint.serializers.adapters.HttpResponseStatusAdapter;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class GsonParser implements Parser<String, Serializable> {

    private final Gson gson;
    private final MediaRange supportedRange = new MediaRange("application/json, text/json");

    public GsonParser(boolean prettyPrint, HashMap<Type, Object> typeaAdapters, List<ExclusionStrategy> exlusionStrategies) {
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
    public String marshall(Serializable src) throws SerializationException {
        try {
            return gson.toJson(src);
        } catch (Exception e) {
            throw new SerializationException("Couldn't marshall to json due to: " + e.getMessage(), e);
        }
    }

    @Override
    public Serializable unMarshall(String object) throws SerializationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MediaRange getSupportedMediaRange() {
        return supportedRange;
    }
}
