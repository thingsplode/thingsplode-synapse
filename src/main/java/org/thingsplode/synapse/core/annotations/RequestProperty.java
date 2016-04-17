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
package org.thingsplode.synapse.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.thingsplode.synapse.core.domain.Request;

/**
 * The value of the method parameter is delivered via a Request Property (the HTTP headers are converted to Request Properties in the {@link Request.RequestHeader});<br>
 * For example: in case the value is "Host" and the request header contains a key/value property [Host : localhost:8080], the string "localhost:8080" will be passed to the method parameter;
 * @author Csaba Tamas
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestProperty {

    /**
     * The request property key, which contains the desired value
     */
    String value();

    /**
     * Whether the parameter is required.
     * <p>
     * Defaults to {@code true}, leading to an exception being thrown if the
     * parameter is missing in the request. Switch this to {@code false} if you
     * prefer a {@code null} value if the parameter is not present in the
     * request.
     * <p>
     * Alternatively, provide a {@link #defaultValue}, which implicitly sets
     * this flag to {@code false}.
     *
     * @return
     */
    boolean required() default true;
}
