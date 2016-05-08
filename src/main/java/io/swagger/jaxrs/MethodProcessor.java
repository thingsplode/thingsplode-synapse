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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;
import io.swagger.annotations.ResponseHeader;
import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.config.ReaderConfig;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Response;
import io.swagger.models.SecurityRequirement;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.ReflectionUtils;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Csaba Tamas
 */
public class MethodProcessor {

    private static final String PATH_DELIMITER = "/";
    private static final String SUCCESSFUL_OPERATION = "successful operation";

    static Operation parseMethod(Class<?> cls, Method method, List<Parameter> globalParameters, Swagger swagger) {
        Operation operation = new Operation();

        ApiOperation apiOperationAnnotation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        ApiResponses responseAnnotation = ReflectionUtils.getAnnotation(method, ApiResponses.class);

        String operationId = method.getName();
        String responseContainer = null;

        Type responseType = null;
        Map<String, Property> defaultResponseHeaders = new HashMap<>();

        if (apiOperationAnnotation != null) {
            if (apiOperationAnnotation.hidden()) {
                return null;
            }
            if (!"".equals(apiOperationAnnotation.nickname())) {
                operationId = apiOperationAnnotation.nickname();
            }

            defaultResponseHeaders = parseResponseHeaders(swagger, apiOperationAnnotation.responseHeaders());

            operation
                    .summary(apiOperationAnnotation.value())
                    .description(apiOperationAnnotation.notes());

            if (apiOperationAnnotation.response() != null && !isVoid(apiOperationAnnotation.response())) {
                responseType = apiOperationAnnotation.response();
            }
            if (!"".equals(apiOperationAnnotation.responseContainer())) {
                responseContainer = apiOperationAnnotation.responseContainer();
            }
            if (apiOperationAnnotation.authorizations() != null) {
                List<SecurityRequirement> securities = new ArrayList<>();
                for (Authorization auth : apiOperationAnnotation.authorizations()) {
                    if (auth.value() != null && !"".equals(auth.value())) {
                        SecurityRequirement security = new SecurityRequirement();
                        security.setName(auth.value());
                        AuthorizationScope[] scopes = auth.scopes();
                        for (AuthorizationScope scope : scopes) {
                            if (scope.scope() != null && !"".equals(scope.scope())) {
                                security.addScope(scope.scope());
                            }
                        }
                        securities.add(security);
                    }
                }
                if (securities.size() > 0) {
                    securities.stream().forEach((sec) -> {
                        operation.security(sec);
                    });
                }
            }
            if (apiOperationAnnotation.consumes() != null && !apiOperationAnnotation.consumes().isEmpty()) {
                operation.consumes(apiOperationAnnotation.consumes());
            }
            if (apiOperationAnnotation.produces() != null && !apiOperationAnnotation.produces().isEmpty()) {
                operation.produces(apiOperationAnnotation.produces());
            }
        }

        if (apiOperationAnnotation != null && StringUtils.isNotEmpty(apiOperationAnnotation.responseReference())) {
            Response response = new Response().description(SUCCESSFUL_OPERATION);
            response.schema(new RefProperty(apiOperationAnnotation.responseReference()));
            operation.addResponse(String.valueOf(apiOperationAnnotation.code()), response);
        } else if (responseType == null) {
            // pick out response from method declaration
            //handle Response types
            if (method.getGenericReturnType() instanceof ParameterizedType 
                    && org.thingsplode.synapse.core.Response.class.isAssignableFrom((Class<?>)((ParameterizedType)method.getGenericReturnType()).getRawType())) {
                responseType = ((ParameterizedType)method.getGenericReturnType()).getActualTypeArguments()[0];
            } else {
                responseType = method.getGenericReturnType();
            }

        }
        if (isValidResponse(responseType)) {
            final Property property = ModelConverters.getInstance().readAsProperty(responseType);
            if (property != null) {
                final Property responseProperty = Reader.ContainerWrapper.wrapContainer(responseContainer, property);
                final int responseCode = apiOperationAnnotation == null ? 200 : apiOperationAnnotation.code();
                operation.response(responseCode, new Response().description(SUCCESSFUL_OPERATION).schema(responseProperty)
                        .headers(defaultResponseHeaders));
                appendModels(swagger, responseType);
            }
        }

        operation.operationId(operationId);

        if (apiOperationAnnotation != null && apiOperationAnnotation.consumes() != null && apiOperationAnnotation.consumes().isEmpty()) {
            //todo: check what to do with consumers
//            final Consumes consumes = ReflectionUtils.getAnnotation(method, Consumes.class);
//            if (consumes != null) {
//                for (String mediaType : ReaderUtils.splitContentValues(consumes.value())) {
//                    operation.consumes(mediaType);
//                }
//            }
        }

        if (apiOperationAnnotation != null && apiOperationAnnotation.produces() != null && apiOperationAnnotation.produces().isEmpty()) {
            //todo: check what to do with produces
//            final Produces produces = ReflectionUtils.getAnnotation(method, Produces.class);
//            if (produces != null) {
//                for (String mediaType : ReaderUtils.splitContentValues(produces.value())) {
//                    operation.produces(mediaType);
//                }
//            }
        }

        List<ApiResponse> apiResponses = new ArrayList<>();
        if (responseAnnotation != null) {
            apiResponses.addAll(Arrays.asList(responseAnnotation.value()));
        }

        Class<?>[] exceptionTypes = method.getExceptionTypes();
        for (Class<?> exceptionType : exceptionTypes) {
            ApiResponses exceptionResponses = ReflectionUtils.getAnnotation(exceptionType, ApiResponses.class);
            if (exceptionResponses != null) {
                apiResponses.addAll(Arrays.asList(exceptionResponses.value()));
            }
        }

        for (ApiResponse apiResponse : apiResponses) {
            Map<String, Property> responseHeaders = parseResponseHeaders(swagger, apiResponse.responseHeaders());

            Response response = new Response()
                    .description(apiResponse.message())
                    .headers(responseHeaders);

            if (apiResponse.code() == 0) {
                operation.defaultResponse(response);
            } else {
                operation.response(apiResponse.code(), response);
            }

            if (StringUtils.isNotEmpty(apiResponse.reference())) {
                response.schema(new RefProperty(apiResponse.reference()));
            } else if (!isVoid(apiResponse.response())) {
                responseType = apiResponse.response();
                final Property property = ModelConverters.getInstance().readAsProperty(responseType);
                if (property != null) {
                    response.schema(Reader.ContainerWrapper.wrapContainer(apiResponse.responseContainer(), property));
                    appendModels(swagger, responseType);
                }
            }
        }
        if (ReflectionUtils.getAnnotation(method, Deprecated.class) != null) {
            operation.setDeprecated(true);
        }

        // process parameters
        //=====================
        globalParameters.stream().forEach((globalParameter) -> {
            operation.parameter(globalParameter);
        });

        ParameterExtractor.getParameters(swagger, cls, method).forEach(p -> operation.parameter(p));

        if (operation.getResponses() == null) {
            Response response = new Response().description(SUCCESSFUL_OPERATION);
            operation.defaultResponse(response);
        }
        return operation;
    }

    private static Map<String, Property> parseResponseHeaders(Swagger swagger, ResponseHeader[] headers) {
        Map<String, Property> responseHeaders = null;
        if (headers != null && headers.length > 0) {
            for (ResponseHeader header : headers) {
                String name = header.name();
                if (!"".equals(name)) {
                    if (responseHeaders == null) {
                        responseHeaders = new HashMap<>();
                    }
                    String description = header.description();
                    Class<?> cls = header.response();

                    if (!isVoid(cls)) {
                        final Property property = ModelConverters.getInstance().readAsProperty(cls);
                        if (property != null) {
                            Property responseProperty = Reader.ContainerWrapper.wrapContainer(header.responseContainer(), property,
                                    Reader.ContainerWrapper.ARRAY, Reader.ContainerWrapper.LIST, Reader.ContainerWrapper.SET);
                            responseProperty.setDescription(description);
                            responseHeaders.put(name, responseProperty);
                            appendModels(swagger, cls);
                        }
                    }
                }
            }
        }
        return responseHeaders;
    }

    private static void appendModels(Swagger swagger, Type type) {
        final Map<String, Model> models = ModelConverters.getInstance().readAll(type);
        models.entrySet().stream().forEach((entry) -> {
            swagger.model(entry.getKey(), entry.getValue());
        });
    }

    private static boolean isVoid(Type type) {
        final Class<?> cls = TypeFactory.defaultInstance().constructType(type).getRawClass();
        return Void.class.isAssignableFrom(cls) || Void.TYPE.isAssignableFrom(cls);
    }

    static boolean isIgnored(String path, ReaderConfig readerConfig) {
        for (String item : readerConfig.getIgnoredRoutes()) {
            final int length = item.length();
            if (path.startsWith(item) && (path.length() == length || path.startsWith(PATH_DELIMITER, length))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidResponse(Type type) {
        if (type == null) {
            return false;
        }
        final JavaType javaType = TypeFactory.defaultInstance().constructType(type);
        if (isVoid(javaType)) {
            return false;
        }
        final Class<?> cls = javaType.getRawClass();
        return !javax.ws.rs.core.Response.class.isAssignableFrom(cls) && !isResourceClass(cls);
    }

    private static boolean isResourceClass(Class<?> cls) {
        return cls.getAnnotation(Api.class) != null;
    }
}
