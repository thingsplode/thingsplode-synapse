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
package org.thingsplode.synapse.serializers.gson.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.lang.reflect.Type;

/**
 *
 * @author Csaba Tamas
 */
public class HttpResponseStatusAdapter implements JsonSerializer<HttpResponseStatus>, JsonDeserializer<HttpResponseStatus> {

    public HttpResponseStatusAdapter() {
    }

    @Override
    public JsonElement serialize(HttpResponseStatus src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("code", new JsonPrimitive(src.code()));
        jsonObject.add("reason", new JsonPrimitive(src.reasonPhrase()));
        return jsonObject;
    }

    @Override
    public HttpResponseStatus deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return new HttpResponseStatus(json.getAsJsonObject().get("code").getAsInt(), json.getAsJsonObject().get("reason").getAsString());

    }

}
