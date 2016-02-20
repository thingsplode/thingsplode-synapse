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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.thingsplode.synapse.core.annotations.RequestMapping;
import org.thingsplode.synapse.core.annotations.Service;
import org.thingsplode.synapse.core.domain.AbstractMessage;

/**
 *
 * @author tamas.csaba@gmail.com
 */
class ServiceRegistry {

    private HashMap<String, Method> routes;

    public void register(Object serviceInstance) {
        if (!serviceInstance.getClass().isAnnotationPresent(Service.class)) {
            throw new IllegalArgumentException("The service instance must be annotated with: " + Service.class.getSimpleName());
        }

    }

    private List<Method> extractMethods(Class clazz) {
        ArrayList<Method> methods = new ArrayList<>();
        for ((Method m :  clazz.getMethods()) {
            if ((m.isAnnotationPresent(RequestMapping.class))
                    || (Arrays.asList(m.getParameterTypes()).contains(AbstractMessage.class))
                    ) {
                    
            }
            }
            return null;
        }
    }
