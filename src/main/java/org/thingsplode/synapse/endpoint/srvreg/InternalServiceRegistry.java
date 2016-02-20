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
package org.thingsplode.synapse.endpoint.srvreg;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.thingsplode.synapse.core.annotations.PathVariable;
import org.thingsplode.synapse.core.annotations.RequestBody;
import org.thingsplode.synapse.core.annotations.RequestMapping;
import org.thingsplode.synapse.core.annotations.RequestParam;
import org.thingsplode.synapse.core.annotations.Service;
import org.thingsplode.synapse.core.domain.AbstractMessage;
import org.thingsplode.synapse.core.domain.RequestMethod;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author tamas.csaba@gmail.com
 */
class InternalServiceRegistry {

    private Map<String, MethodContext> routes = new HashMap<>();

    public void register(Object serviceInstance) {
        Class<?> srvClass = serviceInstance.getClass();
        if (!srvClass.isAnnotationPresent(Service.class)) {
            throw new IllegalArgumentException("The service instance must be annotated with: " + Service.class.getSimpleName());
        }
        String rootContext = srvClass.getAnnotation(Service.class).value();
        if (rootContext.endsWith("/")) {
            rootContext = rootContext.substring(0, rootContext.length() - 1);
        }
        if (Util.isEmpty(rootContext)) {
            rootContext = "/";
        }
        populateMethods(rootContext, srvClass);
        System.out.println("finish");
    }

    private void populateMethods(String rootCtx, Class clazz) {
        for (Method m : clazz.getMethods()) {
            MethodContext mc;
            mc = new MethodContext(rootCtx, m);
            mc.parameters.addAll(processParameters(m));

            if (m.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping rm = m.getAnnotation(RequestMapping.class);
                Arrays.asList(rm.value()).forEach(ru -> {
                    if (!ru.startsWith("/")) {
                        ru = "/" + ru;
                    }
                    String uri = rootCtx + ru;
                    mc.requestMethods.addAll(Arrays.asList(rm.method()));
                    routes.put(uri, mc);
                });
            } else if (containsMessageClass(m.getParameterTypes()) || AbstractMessage.class.isAssignableFrom(m.getReturnType())) {
                String uri = rootCtx + "/" +  m.getName();
                routes.put(uri, mc);
            }
        }
    }

    private List<MethodParam> processParameters(Method m) {
        List<MethodParam> mps = new ArrayList<>();
        if (m.getParameterCount() > 0) {
            Arrays.asList(m.getParameters()).forEach((Parameter p) -> {
                boolean required = true;
                MethodParam<?> mp;

                if (p.isAnnotationPresent(RequestParam.class)) {
                    mp = new MethodParam(p, MethodParam.ParameterSource.URI_PARAM, p.getAnnotation(RequestParam.class).value());
                    required = p.getAnnotation(RequestParam.class).required();
                    if (!Util.isEmpty(p.getAnnotation(RequestParam.class).defaultValue())) {
                        Class valueClass = p.getType();
                        mp.defaultValue = generateDefaultValue(valueClass, p.getAnnotation(RequestParam.class).defaultValue());
                        mp.defaultValueClass = valueClass;
                    }
                } else if (p.isAnnotationPresent(RequestBody.class)) {
                    mp = new MethodParam(p, MethodParam.ParameterSource.BODY, p.getName());
                    required = p.getAnnotation(RequestBody.class).required();
                } else if (p.isAnnotationPresent(PathVariable.class)) {
                    mp = new MethodParam(p, MethodParam.ParameterSource.PATH_VARIABLE, p.getAnnotation(PathVariable.class).value());
                } else if (AbstractMessage.class.isAssignableFrom(p.getType())) {
                    mp = new MethodParam(p, MethodParam.ParameterSource.BODY, p.getName());
                } else {
                    mp = new MethodParam(p, MethodParam.ParameterSource.URI_PARAM, p.getName());
                }
                mp.required = required;
                mp.parameter = p;
                mps.add(mp);
            });
        }
        return mps;
    }

    private boolean containsMessageClass(Class<?>[] array) {
        Optional<Class<?>> result = Arrays.asList(array).stream().filter(c -> AbstractMessage.class.isAssignableFrom(c)).findFirst();
        return result.isPresent();
    }

    private <T> T generateDefaultValue(Class<T> type, String sValue) {
        if (type.equals(Integer.class)) {
            return type.cast(Integer.parseInt(sValue));
        } else if (type.equals(Long.class)) {
            return type.cast(Long.parseLong(sValue));
        } else if (type.equals(String.class)) {
            return type.cast(sValue);
        }
        return null;
    }

    class MethodContext {

        String rootCtx;
        Method method;
        List<RequestMethod> requestMethods = new ArrayList<>();
        List<MethodParam> parameters = new ArrayList<>();

        public MethodContext(String rootCtx, Method method) {
            this.rootCtx = rootCtx;
            this.method = method;
            this.requestMethods = new ArrayList<>();
        }

        public MethodContext(String rootCtx, Method method, List<RequestMethod> requestMethods) {
            if (requestMethods != null) {
                this.rootCtx = rootCtx;
                this.method = method;
                this.requestMethods.addAll(requestMethods);
            } else {
                this.rootCtx = rootCtx;
                this.method = method;
            }
        }

        void addRequestMethod(RequestMethod rm) {
            requestMethods.add(rm);
        }

        void addRequestMethods(List<RequestMethod> rms) {
            requestMethods.addAll(rms);
        }

        void addRequestMethods(RequestMethod[] rms) {
            addRequestMethods(Arrays.asList(rms));
        }
    }

}
