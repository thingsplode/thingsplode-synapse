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
package org.thingsplode.synapse.core.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.thingsplode.synapse.serializers.jackson.adapters.ParameterWrapperDeserializer;

/**
 *
 * @author Csaba Tamas
 */
@JsonDeserialize(using = ParameterWrapperDeserializer.class)
@ApiModel(value = "arameterWrapper", description = "A method parameter wrapper object for the RPC use-case")
public class ParameterWrapper implements Serializable {

    private final ArrayList<Parameter> params = new ArrayList<>();

    public static ParameterWrapper create() {
        return new ParameterWrapper();
    }

    public ParameterWrapper add(String paramid, Class type, Object value) {
        params.add(new Parameter(paramid, type, value));
        return this;
    }

    public List<Parameter> getParams() {
        return Collections.synchronizedList(params);
    }

    public Optional<Parameter> getParameterByName(String name) {
        return params.stream().filter((Parameter p) -> p.paramid.equalsIgnoreCase(name)).findFirst();
    }

    public static class Parameter {

        String paramid;
        Object value;
        Class type;

        @JsonCreator
        public Parameter(@JsonProperty("paramid") String paramid, @JsonProperty("type") Class type, @JsonProperty("value") Object value) {
            this.paramid = paramid;
            this.value = value;
            this.type = type;
        }

        public String getParamid() {
            return paramid;
        }

        public Object getValue() {
            return value;
        }

        public Class getType() {
            return type;
        }

    }
}
