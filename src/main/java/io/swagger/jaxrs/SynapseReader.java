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
import io.swagger.jaxrs.config.ReaderConfig;
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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.SynapseEndpointServiceMarker;
import org.thingsplode.synapse.core.annotations.RequestMapping;
import org.thingsplode.synapse.core.annotations.Service;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class SynapseReader extends Reader {

    private static final String PATH_DELIMITER = "/";
    private static final String SUCCESSFUL_OPERATION = "successful operation";
    private final Logger logger = LoggerFactory.getLogger(SynapseReader.class);

    public SynapseReader(Swagger swagger, ReaderConfig readerConfig) {
        super(swagger, readerConfig);
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
            read(cls, "", null, false, new HashSet<String>(), new HashSet<String>(), new HashMap<String, Tag>(), new ArrayList<Parameter>(), new HashSet<Class<?>>());
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

    private Swagger read(Class<?> cls, String parentPath, String parentMethod, boolean isSubresource, Set<String> parentConsumes, Set<String> parentProduces, Map<String, Tag> parentTags, List<Parameter> parentParameters, Set<Class<?>> scannedResources) {
        Api api = (Api) cls.getAnnotation(Api.class);
        boolean isServiceAnnotated = (cls.getAnnotation(Service.class) != null);
        boolean isMarkedService = SynapseEndpointServiceMarker.class.isAssignableFrom(cls) && !cls.isInterface();

        Map<String, Tag> tags = new HashMap<>();
        List<SecurityRequirement> securities = new ArrayList<>();

        final List<String> consumes = new ArrayList<>();
        final List<String> produces = new ArrayList<>();
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
        final boolean readable
                = ((api != null && (isServiceAnnotated || isMarkedService) && !api.hidden() && !isSubresource)
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
                produces.add(api.produces());
//            } else if (cls.getAnnotation(Produces.class) != null) {
//                produces = ReaderUtils.splitContentValues(cls.getAnnotation(Produces.class).value());
            }
            if (api != null && !api.consumes().isEmpty()) {
                consumes.add(api.consumes());
//            } else if (cls.getAnnotation(Consumes.class) != null) {
//                consumes = ReaderUtils.splitContentValues(cls.getAnnotation(Consumes.class).value());
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
            final Service serviceAnnotation = ReflectionUtils.getAnnotation(cls, Service.class);

            for (Method method : cls.getMethods()) {
                if (ReflectionUtils.isOverriddenMethod(method, cls)) {
                    //is this a method overriden by one in a subclass?
                    continue;
                }

                final RequestMapping requestMappingAnnotation = isServiceAnnotated ? ReflectionUtils.getAnnotation(method, RequestMapping.class) : null;
                final List<String> operationPaths = isServiceAnnotated ? getPaths(serviceAnnotation, method, parentPath) : isMarkedService ? getPaths(method) : null;

                Map<String, String> regexMap = new HashMap<>();

                if (operationPaths == null) {
                    continue;
                }

                operationPaths.stream()
                        .map(op -> PathUtils.parsePath(op, regexMap))
                        .filter(op -> op != null && !isIgnored(op))
                        .forEach(op -> {
                            final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
                            List<String> httpMethods = extractOperationMethods(apiOperation, method, SwaggerExtensions.chain());
                            List<Operation> operations = new ArrayList<>();
                            if (httpMethods != null && !httpMethods.isEmpty()) {
                                httpMethods.forEach(hm -> {
                                    Operation operation = null;
                                    if (apiOperation != null
                                            || getConfig().isScanAllResources()
                                            || (hm != null)
                                            || requestMappingAnnotation != null
                                            || isMarkedService) {
                                        operation = parseMethod(cls, method, globalParameters);
                                    }
                                    if (operation != null) {

                                        if (parentParameters != null && !parentParameters.isEmpty()) {
                                            //add parent parameters to this Operation
                                            for (Parameter param : parentParameters) {
                                                operation.parameter(param);
                                            }
                                        }
                                        prepareOperation(operation, apiOperation, regexMap, globalSchemes);

                                        Set<String> apiConsumes = new HashSet<>(consumes);
                                        if (parentConsumes != null) {
                                            apiConsumes.addAll(parentConsumes);
                                            if (operation.getConsumes() != null) {
                                                apiConsumes.addAll(operation.getConsumes());
                                            }
                                        }

                                        Set<String> apiProduces = new HashSet<>(produces);
                                        if (parentProduces != null) {
                                            apiProduces.addAll(parentProduces);
                                            if (operation.getProduces() != null) {
                                                apiProduces.addAll(operation.getProduces());
                                            }
                                        }

                                        final Class<?> subResource = getSubResourceWithJaxRsSubresourceLocatorSpecs(method);
                                        if (subResource != null && !scannedResources.contains(subResource)) {
                                            scannedResources.add(subResource);
                                            read(subResource, op, hm, true, apiConsumes, apiProduces, tags, operation.getParameters(), scannedResources);

                                            // remove the sub resource so that it can visit it later in another path
                                            // but we have a room for optimization in the future to reuse the scanned result
                                            // by caching the scanned resources in the reader instance to avoid actual scanning
                                            // the the resources again
                                            scannedResources.remove(subResource);
                                        }

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

                                        Path path = getSwagger().getPath(op);
                                        if (path == null) {
                                            path = new Path();
                                            getSwagger().path(op, path);
                                        }
                                        path.set(hm, operation);

                                        readImplicitParameters(method, operation);
                                    }
                                });
                            }

                        });
            }
        }

        return getSwagger();
    }

    private Operation parseMethod(Class<?> cls, Method method, List<Parameter> globalParameters) {
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

            defaultResponseHeaders = parseResponseHeaders(apiOperationAnnotation.responseHeaders());

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
            logger.debug("picking up response class from method " + method);
            responseType = method.getGenericReturnType();
        }
        if (isValidResponse(responseType)) {
            final Property property = ModelConverters.getInstance().readAsProperty(responseType);
            if (property != null) {
                final Property responseProperty = ContainerWrapper.wrapContainer(responseContainer, property);
                final int responseCode = apiOperationAnnotation == null ? 200 : apiOperationAnnotation.code();
                operation.response(responseCode, new Response().description(SUCCESSFUL_OPERATION).schema(responseProperty)
                        .headers(defaultResponseHeaders));
                appendModels(responseType);
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
            getParameters(type, Arrays.asList(paramAnnotations[i])).stream().forEach((parameter) -> {
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
        if (!parameters.isEmpty()) {
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

    List<String> getPaths(Method method) {
        if (SynapseEndpointServiceMarker.class.isAssignableFrom(method.getDeclaringClass())) {
            List<String> pathList = new ArrayList<>();
            pathList.add("/" + method.getDeclaringClass().getName().replaceAll("\\.", "/") + "/" + method.getName());
            return pathList;
        } else {
            return null;
        }
    }

    List<String> getPaths(Service serviceAnnotation, Method method, String parentPath) {
        final List<String> pathList = new ArrayList<>();
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (serviceAnnotation == null && requestMapping == null && StringUtils.isEmpty(parentPath)) {
            //if not a service, request mapping annotation and the parent path is also empty
            return pathList;
        }

        StringBuilder b = new StringBuilder();
        if (parentPath != null && !"".equals(parentPath) && !"/".equals(parentPath)) {
            if (!parentPath.startsWith("/")) {
                parentPath = "/" + parentPath;
            }
            if (parentPath.endsWith("/")) {
                parentPath = parentPath.substring(0, parentPath.length() - 1);
            }

            b.append(parentPath);
        }
        if (serviceAnnotation != null) {
            b.append(serviceAnnotation.value());
        }

        if (requestMapping != null && requestMapping.value().length > 0) {
            Arrays.asList(requestMapping.value()).forEach(methodPath -> {
                String fullPath = b.toString();
                if (!"/".equalsIgnoreCase(methodPath)) {
                    if (!methodPath.startsWith("/") && !b.toString().endsWith("/")) {
                        b.append("/");
                    }
                    if (methodPath.endsWith("/")) {
                        methodPath = methodPath.substring(0, methodPath.length() - 1);
                    }
                    pathList.add(fullPath + methodPath);
                }
            });
        } else if (method.getDeclaringClass().getAnnotation(Service.class) != null) {
            pathList.add(b.append("/").append(method.getName()).toString());
        }

        return pathList.stream()
                .map(o -> !o.startsWith("/") ? "/" + o : o)
                .map(o -> ((o.endsWith("/") && o.length() > 1)) ? o.substring(0, o.length() - 1) : o)
                .collect(Collectors.toList());
    }

    public List<String> extractOperationMethods(ApiOperation apiOperation, Method method, Iterator<SwaggerExtension> chain) {
        ArrayList<String> a = new ArrayList<>();
        if (apiOperation != null && apiOperation.httpMethod() != null && !"".equals(apiOperation.httpMethod())) {
            a.add(apiOperation.httpMethod().toLowerCase());
            return a;
        } else if (method.getAnnotation(RequestMapping.class) != null) {
            List<String> l = Arrays.asList(method.getAnnotation(RequestMapping.class).method()).stream().map(rm -> rm.toString().toLowerCase()).collect(Collectors.toList());
            if (l.isEmpty()) {
                l.add("get");
            }
            return l;
        } else if (SynapseEndpointServiceMarker.class.isAssignableFrom(method.getDeclaringClass())
                || method.getDeclaringClass().getAnnotation(Service.class) != null) {
            a.add("get");
            //todo: enable this when the service registry will support this differentiation
            //if (method.getReturnType().equals(Void.TYPE)) {
            //    a.add("post");
            //} else {
            //    a.add("get");
            //}
            return a;
        } else if ((ReflectionUtils.getOverriddenMethod(method)) != null) {
            return extractOperationMethods(apiOperation, ReflectionUtils.getOverriddenMethod(method), chain);
        } else if (chain != null && chain.hasNext()) {
            a.add(chain.next().extractOperationMethod(apiOperation, method, chain));
            return a;
        } else {
            return a;
        }
    }

    private void prepareOperation(Operation operation, ApiOperation apiOperation, Map<String, String> regexMap, Set<Scheme> globalSchemes) {
        operation.getParameters().stream().filter((param) -> (regexMap.get(param.getName()) != null)).forEach((param) -> {
            String pattern = regexMap.get(param.getName());
            param.setPattern(pattern);
        });

        if (apiOperation != null) {
            parseSchemes(apiOperation.protocols()).stream().forEach((scheme) -> {
                operation.scheme(scheme);
            });
        }

        if (operation.getSchemes() == null || operation.getSchemes().isEmpty()) {
            globalSchemes.stream().forEach((scheme) -> {
                operation.scheme(scheme);
            });
        }
    }
}
