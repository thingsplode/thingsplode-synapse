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
import org.thingsplode.synapse.core.RequestMethod;

/**
 * An optional annotation for methods, which provides the possibility to define:<br>
 * <ul>
 * <li> the exact public name of the method
 * <li> the request method itself
 * </ul>
 * If this annotation is missing:<br>
 * <ul>
 * <li>the java method name will be used
 * <li>the methods will react of GET if return value is provided and there's no need for Request Body, POST will be favored in case of request or path variables are used or request body is required.
 * </ul>
 * @author Csaba Tamas
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {

    /**
     * The names under which this java method should be published;
     * @return
     */
    String[] value() default {};
    
    /**
     * The Rest Methods to which this java method should be invoked
     * @return
     */
    RequestMethod[] method() default {};
}
