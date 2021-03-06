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
package org.thingsplode.synapse.endpoint;

import java.lang.reflect.Parameter;
import java.util.regex.Pattern;

/**
 *
 * @author Csaba Tamas
 */
class MethodParam<T> {

    ParameterSource source;
    /**
     * the unique id of the parameter (usually the value of the HeaderParam/PathVariable/RequestParam annotation
     */
    String paramId;
    Parameter parameter;
    Object defaultValue;
    Class defaultValueClass;
    Pattern pathVariableMatcher;
    boolean pathVariableOnRootContext = true;
    boolean required = true;

    public MethodParam(Parameter param, ParameterSource source, String paramId) {
        this.source = source;
        this.paramId = paramId;
    }

    enum ParameterSource {
        PATH_VARIABLE,
        HEADER_PARAM,
        QUERY_PARAM,
        BODY,
        PARAMETER_WRAPPER
    }
}
