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
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;
import io.swagger.annotations.ResponseHeader;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.jaxrs.ext.SwaggerExtensions;
import io.swagger.jaxrs.utils.ReaderUtils;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.SecurityRequirement;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.BaseReaderUtils;
import io.swagger.util.ParameterProcessor;
import io.swagger.util.PathUtils;
import io.swagger.util.ReflectionUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.annotations.Service;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class SynapseReader extends Reader {

    private static final String PATH_DELIMITER = "/";
    private static final String SUCCESSFUL_OPERATION = "successful operation";
    private final Logger logger = LoggerFactory.getLogger(SynapseReader.class);

    public SynapseReader(Swagger swagger) {
        super(swagger);
    }

    @Override
    public Swagger read(Set<Class<?>> classes) {

        Map<Class<?>, ReaderListener> listeners = new HashMap<>();

        classes.stream().filter((cls) -> (ReaderListener.class.isAssignableFrom(cls) && !listeners.containsKey(cls))).forEach((cls) -> {
            try {
                listeners.put(cls, (ReaderListener) cls.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Failed to create ReaderListener", e);
            }
        });

        listeners.values().stream().forEach((listener) -> {
            try {
                listener.beforeScan(this, getSwagger());
            } catch (Exception e) {
                logger.error("Unexpected error invoking beforeScan listener [" + listener.getClass().getName() + "]", e);
            }
        });

        // process SwaggerDefinitions first - so we get tags in desired order
        classes.stream().forEach((cls) -> {
            SwaggerDefinition swaggerDefinition = cls.getAnnotation(SwaggerDefinition.class);
            if (swaggerDefinition != null) {
                readSwaggerConfig(cls, swaggerDefinition);
            }
        });

        classes.stream().forEach((cls) -> {
            read(cls, "", null, false, new String[0], new String[0], new HashMap<String, Tag>(), new ArrayList<Parameter>(), new HashSet<Class<?>>());
        });

        listeners.values().stream().forEach((listener) -> {
            try {
                listener.afterScan(this, getSwagger());
            } catch (Exception e) {
                logger.error("Unexpected error invoking afterScan listener [" + listener.getClass().getName() + "]", e);
            }
        });

        return getSwagger();
    }

    private Swagger read(Class<?> cls, String parentPath, String parentMethod, boolean isSubresource, String[] parentConsumes, String[] parentProduces, Map<String, Tag> parentTags, List<Parameter> parentParameters, Set<Class<?>> scannedResources) {
        Api api = (Api) cls.getAnnotation(Api.class);
        boolean hasServiceAnnotation = (cls.getAnnotation(Service.class) != null);

        Map<String, Tag> tags = new HashMap<>();
        List<SecurityRequirement> securities = new ArrayList<>();

        String[] consumes = new String[0];
        String[] produces = new String[0];
        final Set<Scheme> globalSchemes = EnumSet.noneOf(Scheme.class);

        /*
         *   Only read @Api configuration if:
         *
         *   @Api annotated AND
         *   @Path annotated AND
         *   @Api (hidden) false
         *   isSubresource false
         *
         *   OR
         *
         *   @Api annotated AND
         *   isSubresource true
         *   @Api (hidden) false
         *
         */
        final boolean readable = hasServiceAnnotation 
                || ((api != null && hasServiceAnnotation && !api.hidden() && !isSubresource)
                || (api != null && !api.hidden() && isSubresource)
                || (api != null && !api.hidden() && getConfig().isScanAllResources()));

        if (readable) {
            // the value will be used as a tag for 2.0 UNLESS a Tags annotation is present
            Set<String> tagStrings = extractTags(api);
            tagStrings.stream().forEach((tagString) -> {
                Tag tag = new Tag().name(tagString);
                tags.put(tagString, tag);
            });
            tags.keySet().stream().forEach((tagName) -> {
                getSwagger().tag(tags.get(tagName));
            });

            if (api != null && !api.produces().isEmpty()) {
                produces = new String[]{api.produces()};
            } else if (cls.getAnnotation(Produces.class) != null) {
                produces = ReaderUtils.splitContentValues(cls.getAnnotation(Produces.class).value());
            }
            if (api != null && !api.consumes().isEmpty()) {
                consumes = new String[]{api.consumes()};
            } else if (cls.getAnnotation(Consumes.class) != null) {
                consumes = ReaderUtils.splitContentValues(cls.getAnnotation(Consumes.class).value());
            }
            if (api != null) {
                globalSchemes.addAll(parseSchemes(api.protocols()));
            }
            Authorization[] authorizations = api != null ? api.authorizations() : new Authorization[]{};

            for (Authorization auth : authorizations) {
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
        }

        if (isSubresource) {
            if (parentTags != null) {
                tags.putAll(parentTags);
            }
        }

        if (readable || (api == null && getConfig().isScanAllResources())) {
            // merge consumes, produces

            // look for method-level annotated properties
            // handle sub-resources by looking at return type
            final List<Parameter> globalParameters = new ArrayList<>();

            // look for constructor-level annotated properties
            globalParameters.addAll(ReaderUtils.collectConstructorParameters(cls, getSwagger()));

            // look for field-level annotated properties
            globalParameters.addAll(ReaderUtils.collectFieldParameters(cls, getSwagger()));

            // parse the method
            final javax.ws.rs.Path apiPath = ReflectionUtils.getAnnotation(cls, javax.ws.rs.Path.class);
            Method methods[] = cls.getMethods();
            for (Method method : methods) {
                if (ReflectionUtils.isOverriddenMethod(method, cls)) {
                    continue;
                }
                javax.ws.rs.Path methodPath = ReflectionUtils.getAnnotation(method, javax.ws.rs.Path.class);

                String operationPath = getPath(apiPath, methodPath, parentPath);
                Map<String, String> regexMap = new HashMap<>();
                operationPath = PathUtils.parsePath(operationPath, regexMap);
                if (operationPath != null) {
                    if (isIgnored(operationPath)) {
                        continue;
                    }

                    final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
                    String httpMethod = extractOperationMethod(apiOperation, method, SwaggerExtensions.chain());

                    Operation operation = null;
                    if (apiOperation != null || getConfig().isScanAllResources() || httpMethod != null || methodPath != null) {
                        operation = parseMethod(cls, method, globalParameters);
                    }
                    if (operation == null) {
                        continue;
                    }
                    if (parentParameters != null) {
                        for (Parameter param : parentParameters) {
                            operation.parameter(param);
                        }
                    }
                    for (Parameter param : operation.getParameters()) {
                        if (regexMap.get(param.getName()) != null) {
                            String pattern = regexMap.get(param.getName());
                            param.setPattern(pattern);
                        }
                    }

                    if (apiOperation != null) {
                        for (Scheme scheme : parseSchemes(apiOperation.protocols())) {
                            operation.scheme(scheme);
                        }
                    }

                    if (operation.getSchemes() == null || operation.getSchemes().isEmpty()) {
                        for (Scheme scheme : globalSchemes) {
                            operation.scheme(scheme);
                        }
                    }

                    String[] apiConsumes = consumes;
                    if (parentConsumes != null) {
                        Set<String> both = new HashSet<>(Arrays.asList(apiConsumes));
                        both.addAll(new HashSet<>(Arrays.asList(parentConsumes)));
                        if (operation.getConsumes() != null) {
                            both.addAll(new HashSet<>(operation.getConsumes()));
                        }
                        apiConsumes = both.toArray(new String[both.size()]);
                    }

                    String[] apiProduces = produces;
                    if (parentProduces != null) {
                        Set<String> both = new HashSet<>(Arrays.asList(apiProduces));
                        both.addAll(new HashSet<>(Arrays.asList(parentProduces)));
                        if (operation.getProduces() != null) {
                            both.addAll(new HashSet<>(operation.getProduces()));
                        }
                        apiProduces = both.toArray(new String[both.size()]);
                    }
                    final Class<?> subResource = getSubResourceWithJaxRsSubresourceLocatorSpecs(method);
                    if (subResource != null && !scannedResources.contains(subResource)) {
                        scannedResources.add(subResource);
                        read(subResource, operationPath, httpMethod, true, apiConsumes, apiProduces, tags, operation.getParameters(), scannedResources);
                        // remove the sub resource so that it can visit it later in another path
                        // but we have a room for optimization in the future to reuse the scanned result
                        // by caching the scanned resources in the reader instance to avoid actual scanning
                        // the the resources again
                        scannedResources.remove(subResource);
                    }

                    // can't continue without a valid http method
                    httpMethod = httpMethod == null ? parentMethod : httpMethod;
                    if (httpMethod != null) {
                        if (apiOperation != null) {
                            boolean hasExplicitTag = false;
                            for (String tag : apiOperation.tags()) {
                                if (!"".equals(tag)) {
                                    operation.tag(tag);
                                    getSwagger().tag(new Tag().name(tag));
                                }
                            }

                            operation.getVendorExtensions().putAll(BaseReaderUtils.parseExtensions(apiOperation.extensions()));
                        }

                        if (operation.getConsumes() == null) {
                            for (String mediaType : apiConsumes) {
                                operation.consumes(mediaType);
                            }
                        }
                        if (operation.getProduces() == null) {
                            for (String mediaType : apiProduces) {
                                operation.produces(mediaType);
                            }
                        }

                        if (operation.getTags() == null) {
                            for (String tagString : tags.keySet()) {
                                operation.tag(tagString);
                            }
                        }
                        // Only add global @Api securities if operation doesn't already have more specific securities
                        if (operation.getSecurity() == null) {
                            for (SecurityRequirement security : securities) {
                                operation.security(security);
                            }
                        }

                        Path path = getSwagger().getPath(operationPath);
                        if (path == null) {
                            path = new Path();
                            getSwagger().path(operationPath, path);
                        }
                        path.set(httpMethod, operation);

                        readImplicitParameters(method, operation);
                    }
                }
            }
        }

        return getSwagger();
    }

    private Operation parseMethod(Class<?> cls, Method method, List<Parameter> globalParameters) {
        Operation operation = new Operation();

        ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        ApiResponses responseAnnotation = ReflectionUtils.getAnnotation(method, ApiResponses.class);

        String operationId = method.getName();
        String responseContainer = null;

        Type responseType = null;
        Map<String, Property> defaultResponseHeaders = new HashMap<>();

        if (apiOperation != null) {
            if (apiOperation.hidden()) {
                return null;
            }
            if (!"".equals(apiOperation.nickname())) {
                operationId = apiOperation.nickname();
            }

            defaultResponseHeaders = parseResponseHeaders(apiOperation.responseHeaders());

            operation
                    .summary(apiOperation.value())
                    .description(apiOperation.notes());

            if (apiOperation.response() != null && !isVoid(apiOperation.response())) {
                responseType = apiOperation.response();
            }
            if (!"".equals(apiOperation.responseContainer())) {
                responseContainer = apiOperation.responseContainer();
            }
            if (apiOperation.authorizations() != null) {
                List<SecurityRequirement> securities = new ArrayList<>();
                for (Authorization auth : apiOperation.authorizations()) {
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
            if (apiOperation.consumes() != null && !apiOperation.consumes().isEmpty()) {
                operation.consumes(apiOperation.consumes());
            }
            if (apiOperation.produces() != null && !apiOperation.produces().isEmpty()) {
                operation.produces(apiOperation.produces());
            }
        }

        if (apiOperation != null && StringUtils.isNotEmpty(apiOperation.responseReference())) {
            Response response = new Response().description(SUCCESSFUL_OPERATION);
            response.schema(new RefProperty(apiOperation.responseReference()));
            operation.addResponse(String.valueOf(apiOperation.code()), response);
        } else if (responseType == null) {
            // pick out response from method declaration
            logger.debug("picking up response class from method " + method);
            responseType = method.getGenericReturnType();
        }
        if (isValidResponse(responseType)) {
            final Property property = ModelConverters.getInstance().readAsProperty(responseType);
            if (property != null) {
                final Property responseProperty = ContainerWrapper.wrapContainer(responseContainer, property);
                final int responseCode = apiOperation == null ? 200 : apiOperation.code();
                operation.response(responseCode, new Response().description(SUCCESSFUL_OPERATION).schema(responseProperty)
                        .headers(defaultResponseHeaders));
                appendModels(responseType);
            }
        }

        operation.operationId(operationId);

        if (apiOperation != null && apiOperation.consumes() != null && apiOperation.consumes().isEmpty()) {
            final Consumes consumes = ReflectionUtils.getAnnotation(method, Consumes.class);
            if (consumes != null) {
                for (String mediaType : ReaderUtils.splitContentValues(consumes.value())) {
                    operation.consumes(mediaType);
                }
            }
        }

        if (apiOperation != null && apiOperation.produces() != null && apiOperation.produces().isEmpty()) {
            final Produces produces = ReflectionUtils.getAnnotation(method, Produces.class);
            if (produces != null) {
                for (String mediaType : ReaderUtils.splitContentValues(produces.value())) {
                    operation.produces(mediaType);
                }
            }
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
            Map<String, Property> responseHeaders = parseResponseHeaders(apiResponse.responseHeaders());

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
                    response.schema(ContainerWrapper.wrapContainer(apiResponse.responseContainer(), property));
                    appendModels(responseType);
                }
            }
        }
        if (ReflectionUtils.getAnnotation(method, Deprecated.class) != null) {
            operation.setDeprecated(true);
        }

        // process parameters
        globalParameters.stream().forEach((globalParameter) -> {
            operation.parameter(globalParameter);
        });

        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < genericParameterTypes.length; i++) {
            final Type type = TypeFactory.defaultInstance().constructType(genericParameterTypes[i], cls);
            List<Parameter> parameters = getParameters(type, Arrays.asList(paramAnnotations[i]));

            parameters.stream().forEach((parameter) -> {
                operation.parameter(parameter);
            });
        }

        if (operation.getResponses() == null) {
            Response response = new Response().description(SUCCESSFUL_OPERATION);
            operation.defaultResponse(response);
        }
        return operation;
    }

    private List<Parameter> getParameters(Type type, List<Annotation> annotations) {
        final Iterator<SwaggerExtension> chain = SwaggerExtensions.chain();
        if (!chain.hasNext()) {
            return Collections.emptyList();
        }
        logger.debug("getParameters for " + type);
        Set<Type> typesToSkip = new HashSet<>();
        final SwaggerExtension extension = chain.next();
        logger.debug("trying extension " + extension);

        final List<Parameter> parameters = extension.extractParameters(annotations, type, typesToSkip, chain);
        if (parameters.size() > 0) {
            final List<Parameter> processed = new ArrayList<>(parameters.size());
            parameters.stream().filter((parameter) -> (ParameterProcessor.applyAnnotations(getSwagger(), parameter, type, annotations) != null)).forEach((parameter) -> {
                processed.add(parameter);
            });
            return processed;
        } else {
            logger.debug("no parameter found, looking at body params");
            final List<Parameter> body = new ArrayList<>();
            if (!typesToSkip.contains(type)) {
                Parameter param = ParameterProcessor.applyAnnotations(getSwagger(), null, type, annotations);
                if (param != null) {
                    body.add(param);
                }
            }
            return body;
        }
    }

    private Map<String, Property> parseResponseHeaders(ResponseHeader[] headers) {
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
                            Property responseProperty = ContainerWrapper.wrapContainer(header.responseContainer(), property,
                                    ContainerWrapper.ARRAY, ContainerWrapper.LIST, ContainerWrapper.SET);
                            responseProperty.setDescription(description);
                            responseHeaders.put(name, responseProperty);
                            appendModels(cls);
                        }
                    }
                }
            }
        }
        return responseHeaders;
    }

    private static Set<Scheme> parseSchemes(String schemes) {
        final Set<Scheme> result = EnumSet.noneOf(Scheme.class);
        for (String item : StringUtils.trimToEmpty(schemes).split(",")) {
            final Scheme scheme = Scheme.forValue(StringUtils.trimToNull(item));
            if (scheme != null) {
                result.add(scheme);
            }
        }
        return result;
    }

    private void readImplicitParameters(Method method, Operation operation) {
        ApiImplicitParams implicitParams = method.getAnnotation(ApiImplicitParams.class);
        if (implicitParams != null && implicitParams.value().length > 0) {
            for (ApiImplicitParam param : implicitParams.value()) {
                Parameter p = readImplicitParam(param);
                if (p != null) {
                    operation.addParameter(p);
                }
            }
        }
    }

    private void appendModels(Type type) {
        final Map<String, Model> models = ModelConverters.getInstance().readAll(type);
        models.entrySet().stream().forEach((entry) -> {
            getSwagger().model(entry.getKey(), entry.getValue());
        });
    }

    private static boolean isVoid(Type type) {
        final Class<?> cls = TypeFactory.defaultInstance().constructType(type).getRawClass();
        return Void.class.isAssignableFrom(cls) || Void.TYPE.isAssignableFrom(cls);
    }

    private boolean isIgnored(String path) {
        for (String item : getConfig().getIgnoredRoutes()) {
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
