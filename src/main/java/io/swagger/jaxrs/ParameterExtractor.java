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
package io.swagger.jaxrs;

import com.fasterxml.jackson.databind.type.TypeFactory;
import io.swagger.converter.ModelConverters;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.ParameterProcessor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.thingsplode.synapse.core.SynapseEndpointServiceMarker;
import org.thingsplode.synapse.core.annotations.HeaderParam;
import org.thingsplode.synapse.core.annotations.PathVariable;
import org.thingsplode.synapse.core.annotations.RequestBody;
import org.thingsplode.synapse.core.annotations.RequestParam;
import org.thingsplode.synapse.core.annotations.Service;
import org.thingsplode.synapse.core.domain.ParameterWrapper;
import org.thingsplode.synapse.core.domain.Request;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class ParameterExtractor {

    public static List<Parameter> getParameters(Swagger swagger, Class cls, Method method) {
        List<Parameter> parameters = new ArrayList<>();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < genericParameterTypes.length; i++) {
            Type type = null;
            if (SynapseEndpointServiceMarker.class.isAssignableFrom(cls)) {
                //an RPC style endpoint (implements SynapseEndpointServiceMarker)
                type = TypeFactory.defaultInstance().constructType(ParameterWrapper.class, cls);
            } else if (cls.getAnnotation(Service.class) != null) {
                //an endpoint with @Service annotation
                if (Request.class.isAssignableFrom(genericParameterTypes[i].getClass())) {
                    type = TypeFactory.defaultInstance().constructType(((ParameterizedType) genericParameterTypes[i]).getRawType(), cls);
//                    if (genericParameterTypes[i] instanceof ParameterizedType) {
//                        //the parameter is something like: Request<Tuple<Integer, Integer>> req
//                        type = TypeFactory.defaultInstance().constructType(((ParameterizedType) genericParameterTypes[i]).getRawType(), cls);
//                    } else {
//                        //the parameter is something like: Request<Address> req
//                        type = TypeFactory.defaultInstance().constructType(genericParameterTypes[i], cls);
//                    }
                }

            }

            extractParameters(swagger, parameters, type, Arrays.asList(paramAnnotations[i]));
        }
        return parameters;
    }

    private static void extractParameters(Swagger swagger, List<Parameter> params, Type type, List<Annotation> annotations) {

        boolean annotated = false;
        for (Annotation annotation : annotations) {
            if (annotation instanceof RequestParam) {
                RequestParam param = (RequestParam) annotation;
                QueryParameter qp = new QueryParameter()
                        .name(param.value());
                qp.setRequired(param.required());

                Property schema = createProperty(type);
                if (schema != null) {
                    qp.setProperty(schema);
                }
                annotated = true;
                params.add(ParameterProcessor.applyAnnotations(swagger, qp, type, annotations));
            } else if (annotation instanceof PathVariable) {
                PathVariable param = (PathVariable) annotation;
                PathParameter pp = new PathParameter()
                        .name(param.value());
                Property schema = createProperty(type);
                if (schema != null) {
                    pp.setProperty(schema);
                }
                annotated = true;
                params.add(ParameterProcessor.applyAnnotations(swagger, pp, type, annotations));
            } else if (annotation instanceof HeaderParam) {
                HeaderParam param = (HeaderParam) annotation;
                HeaderParameter hp = new HeaderParameter()
                        .name(param.value());
                Property schema = createProperty(type);
                if (schema != null) {
                    hp.setProperty(schema);
                }
                annotated = true;
                params.add(ParameterProcessor.applyAnnotations(swagger, hp, type, annotations));
            } else if (annotation instanceof RequestBody) {
                RequestBody reqBodyParam = (RequestBody) annotation;
                BodyParameter bp = new BodyParameter();
                bp.setRequired(reqBodyParam.required());
                annotated = true;
                //todo: the apply annotation overwrites the required field
                params.add(ParameterProcessor.applyAnnotations(swagger, bp, type, annotations));
            }
        }
        if (!annotated) {
            Parameter p = ParameterProcessor.applyAnnotations(swagger, null, type, annotations);
            if (p != null) {
                params.add(p);
            }
        }
    }

    private static Property createProperty(Type type) {
        return enforcePrimitive(ModelConverters.getInstance().readAsProperty(type), 0);
    }

    private static Property enforcePrimitive(Property in, int level) {
        if (in instanceof RefProperty) {
            return new StringProperty();
        }
        if (in instanceof ArrayProperty) {
            if (level == 0) {
                final ArrayProperty array = (ArrayProperty) in;
                array.setItems(enforcePrimitive(array.getItems(), level + 1));
            } else {
                return new StringProperty();
            }
        }
        return in;
    }

//    private static boolean shouldIgnoreType(Type type, Set<Type> typesToSkip) {
//        if (typesToSkip.contains(type)) {
//            return true;
//        }
//        if (shouldIgnoreClass(constructType(type).getRawClass())) {
//            typesToSkip.add(type);
//            return true;
//        }
//        return false;
//    }
//
//    private static boolean shouldIgnoreClass(Class<?> cls) {
//        return cls.getName().startsWith("javax.ws.rs.");
//    }
//
//    private static JavaType constructType(Type type) {
//        return TypeFactory.defaultInstance().constructType(type);
//    }
}
