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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class ParameterWrapper {

    private final ArrayList<Parameter> params = new ArrayList<>();

//    public ParameterWrapper(Method method) {
//        Arrays.asList(method.getParameters()).forEach(p -> {
//            params.add(new Parameter(p.getName(), p.getType(), ));
//        });
//    }
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

    public class Parameter {

        String paramid;
        Object value;
        Class type;

        public Parameter(String paramid, Class type, Object value) {
            this.paramid = paramid;
            this.value = value;
        }

        public String getParamid() {
            return paramid;
        }

        public void setParamid(String paramid) {
            this.paramid = paramid;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

    }
}
