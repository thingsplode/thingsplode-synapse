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

import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.SynapseEndpointServiceMarker;
import org.thingsplode.synapse.core.domain.Uri;
import org.thingsplode.synapse.core.annotations.PathVariable;
import org.thingsplode.synapse.core.annotations.RequestBody;
import org.thingsplode.synapse.core.annotations.RequestMapping;
import org.thingsplode.synapse.core.annotations.RequestParam;
import org.thingsplode.synapse.core.annotations.Service;
import org.thingsplode.synapse.core.domain.AbstractMessage;
import org.thingsplode.synapse.core.domain.Event;
import org.thingsplode.synapse.core.domain.MediaType;
import org.thingsplode.synapse.core.domain.ParameterWrapper;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.RequestMethod;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.exceptions.ExecutionException;
import org.thingsplode.synapse.core.exceptions.MethodNotFoundException;
import org.thingsplode.synapse.core.exceptions.MissingParameterException;
import org.thingsplode.synapse.core.exceptions.SerializationException;
import org.thingsplode.synapse.serializers.SerializationService;
import org.thingsplode.synapse.util.Reflector;
import org.thingsplode.synapse.util.Util;
import org.thingsplode.synapse.core.annotations.RequestProperty;

/**
 * The main storage of all services published within the Endpoint. Responsible
 * to find and execute the desired method on the desired service identified by
 * the path.
 *
 * @author Csaba Tamas
 */
public class ServiceRegistry {

    private Routes routes;
    private Pattern urlParamPattern = Pattern.compile("\\{(.*?)\\}", Pattern.CASE_INSENSITIVE);
    private SerializationService serializationService = new SerializationService();
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    public ServiceRegistry() {
        routes = new Routes();
    }

    public Set<String> getRouteExpressions() {
        if (logger.isTraceEnabled()) {
            logger.trace(descriptor(routes.rootCtxes.keySet(), "Route Expressions"));
        }
        return routes.rootCtxes.keySet();
    }

    public Set<String> getAllSupportedPaths() {
        if (logger.isTraceEnabled()) {
            logger.trace(descriptor(routes.paths, "Supported Paths"));
        }
        return routes.paths;
    }

    /**
     * A flag which will describe how to process the body
     */
    private enum WrapFlag {
        /**
         * Don't wrap the body
         */
        NONE,
        /**
         * Body to be handled as a Synapse Request
         */
        REQUEST,
        /**
         * Body to be handled as an event
         */
        EVENT
    }

    /**
     * Returns the method which corresponds best to the {@link Uri} and the
     * {@link RequestMethod}.
     *
     * @param header
     * @param requestBody the
     * @return an {@link Optional<Method>} filled with the method if one was
     * found. Otherwise the mcOpt.isPresent() is false;
     * @throws org.thingsplode.synapse.core.exceptions.ExecutionException
     * @throws org.thingsplode.synapse.core.exceptions.MissingParameterException
     * @throws org.thingsplode.synapse.core.exceptions.SerializationException
     */
    public Response invokeWithParsable(Request.RequestHeader header, Object requestBody) throws ExecutionException, MissingParameterException, SerializationException {

        Optional<MethodContext> mcOpt = getMethodContext(header);
        if (!mcOpt.isPresent()) {
            return new Response(new Response.ResponseHeader(header, HttpResponseStatus.NOT_FOUND, new MediaType("text/plain; charset=UTF-8")));
        }

        Object requestBodyObject = null;
        if ((requestBody instanceof String) && !Util.isEmpty((String) requestBody)) {
            Optional<MethodParam> mpo = mcOpt.get().getMethodParamForRequestBody();
            WrapFlag wrapBodyFlag = WrapFlag.NONE;
            if (mpo.isPresent()) {
                MethodParam mp = mpo.get();
                //if endpoint marker is used, we expect a parameter wrapper
                Class clazz = mp.source == MethodParam.ParameterSource.PARAMETER_WRAPPER ? ParameterWrapper.class : mp.parameter.getType();
                if (mcOpt.get().serviceInstance instanceof AbstractEventSink) {
                    wrapBodyFlag = WrapFlag.EVENT;
                    clazz = ((AbstractEventSink) mcOpt.get().serviceInstance).getClazz();
                } else if (Request.class.isAssignableFrom(clazz)) {
                    //if the parameter is a Request object (eg. Request<Tuple<Integer, Integer>> req), we need to construct it
                    wrapBodyFlag = WrapFlag.REQUEST;
                    if (mp.parameter.getParameterizedType() instanceof ParameterizedType) {
                        Type t = (((ParameterizedType) mp.parameter.getParameterizedType()).getActualTypeArguments()[0]);
                        if (t instanceof ParameterizedType) {
                            //the parameter is something like: Request<Tuple<Integer, Integer>> req
                            clazz = (Class<?>) ((ParameterizedType) t).getRawType();
                        } else if (t instanceof Class) {
                            //the parameter is something like: Request<Address> req
                            clazz = (Class<?>) t;
                        }
                    }
                }

                requestBodyObject = serializationService.getPreferredSerializer(null).unMarshall(clazz, (String) requestBody);
                if (wrapBodyFlag == WrapFlag.REQUEST) {
                    requestBodyObject = new Request(header, (Serializable) requestBodyObject);
                } else if (wrapBodyFlag == WrapFlag.EVENT) {
                    requestBodyObject = new Event(header, (Serializable) requestBodyObject);
                }
            }
        }
        return invoke(header, mcOpt.get(), requestBodyObject);
    }

    public Response invokeWithObject(Request.RequestHeader header, Object requestBody) throws MethodNotFoundException, ExecutionException, MissingParameterException, SerializationException {
        return invoke(header, getMethodContextOrThrowException(header), requestBody);
    }

    Response invoke(Request.RequestHeader header, MethodContext mc, Object requestBodyObject) throws MissingParameterException, ExecutionException {
        try {
            Object result = mc.method.invoke(mc.serviceInstance, mc.extractInvocationArguments(header, requestBodyObject));
            if (result == null) {
                return new Response(new Response.ResponseHeader(header, HttpResponseStatus.OK));
            } else if (result instanceof Response) {
                ((Response) result).getHeader().setCorrelationId(header.getMsgId());
                return (Response) result;
            } else if (result instanceof Serializable) {
                return new Response(new Response.ResponseHeader(header, HttpResponseStatus.OK), (Serializable) result);
            } else {
                throw new ExecutionException("The servive method return type is not serializable.");
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new ExecutionException(header, ex);
        }
    }

    private MethodContext getMethodContextOrThrowException(Request.RequestHeader header) throws MethodNotFoundException {
        Optional<MethodContext> mcOpt = getMethodContext(header);
        if (!mcOpt.isPresent()) {
            throw new MethodNotFoundException(header);
        }
        return mcOpt.get();
    }

    Optional<MethodContext> getMethodContext(Request.RequestHeader header) {
        String methodIdentifierPattern = null;
        String pattern = null;
        for (Pattern p : routes.patterns) {
            Matcher m = p.matcher(header.getUri().getPath());
            if (m.find()) {
                methodIdentifierPattern = header.getUri().getPath().substring(m.end());
                if (methodIdentifierPattern.startsWith("/")) {
                    methodIdentifierPattern = methodIdentifierPattern.substring(1);
                }
                pattern = p.pattern();
                break;
            }
        }
        if (Util.isEmpty(methodIdentifierPattern)) {
            if (logger.isDebugEnabled()) {
                logger.warn("Returning empty Optional<MethodContext> for header: " + header);
            }
            return Optional.empty();
        }

        methodIdentifierPattern = methodIdentifierPattern + header.getUri().createParameterExpression();

        SortedMap<String, MethodContext> methods = routes.rootCtxes.get(pattern);
        MethodContext mc = methods.get(methodIdentifierPattern);
        if (mc == null) {
            final String variableMethodIdentifier = methodIdentifierPattern;
            Optional<String> variableMethodKey = methods.keySet().stream().filter(k -> {
                if (urlParamPattern.matcher(k).find()) {
                    final String keyMatcher = k.replaceAll("\\{.*\\}", "[^/]+");
                    if (Pattern.compile(keyMatcher).matcher(variableMethodIdentifier).find()) {
                        return true;
                    }
                }
                return false;
            }).findFirst();
            if (variableMethodKey.isPresent()) {
                mc = methods.get(variableMethodKey.get());
            }
        }
        if (mc == null || (!mc.requestMethods.isEmpty() && !mc.requestMethods.contains(header.getMethod()))) {
            if (logger.isDebugEnabled()) {
                logger.warn("Returning empty Optional<MethodContext> (because method context or request methods are not available) for header: " + header);
                logger.trace("Method Context (mc): " + (mc == null ? "null" : mc.rootCtx));
                logger.trace("Method identifier pattern: " + methodIdentifierPattern);
                logger.trace("Pattern: " + pattern);
                logger.trace(descriptor(methods.keySet(), "methods"));

                if (mc != null) {
                    logger.trace(descriptor(mc.requestMethods, "Request Methods on the Method Context"));
                }
            }
            return Optional.empty();
        }

        return Optional.of(mc);
    }

    private String descriptor(Collection<?> keys, String title) {
        StringBuilder sbm = new StringBuilder("--- " + title + " --- \n");
        keys.stream().forEach(k -> sbm.append("\t -> ").append(k).append("\n"));
        sbm.append("----");
        return sbm.toString();
    }

    public void register(String path, Object serviceInstance) {
        //todo: make validation to expose Request Mapping or Path Varibale types where conversion from String to method parameter type is possible
        Class<?> srvClass = serviceInstance.getClass();
        String rootContext;
        List<Class> markedInterfaces = Reflector.filterInterfaces(Reflector.extractInterfaces(srvClass), SynapseEndpointServiceMarker.class);
        Set<Method> methods = new HashSet<>();
        if (!srvClass.isAnnotationPresent(Service.class)) {
            if ((markedInterfaces == null || markedInterfaces.isEmpty())) {
                throw new IllegalArgumentException("The service instance must be annotated with: " + Service.class.getSimpleName() + " or the " + SynapseEndpointServiceMarker.class.getSimpleName() + " marker interface must be used.");
            }
            //Marked with marker interface
            rootContext = Util.isEmpty(path) ? "/" + srvClass.getCanonicalName().replaceAll("\\.", "/") : path;
            markedInterfaces.forEach(i -> {
                methods.addAll(Arrays.asList(i.getMethods()));
            });
            methods.addAll(Arrays.asList(srvClass.getDeclaredMethods())
                    .stream()
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .collect(Collectors.toList()));
        } else {
            //Annotated with @Service
            rootContext = Util.isEmpty(path) ? srvClass.getAnnotation(Service.class).value() : path;
            methods.addAll(Arrays.asList(srvClass.getMethods()).stream()
                    .filter(m -> {
                        return m.isAnnotationPresent(RequestMapping.class) || containsMessageClass(m.getParameterTypes()) || AbstractMessage.class.isAssignableFrom(m.getReturnType());
                    })
                    .collect(Collectors.toList()));
        }

        if (rootContext.endsWith("/")) {
            rootContext = rootContext.substring(0, rootContext.length() - 1);
        }
        if (Util.isEmpty(rootContext)) {
            rootContext = "/";
        }
        logger.debug("Registering [" + serviceInstance.getClass().getName() + "] at: " + rootContext);
        populateMethods(rootContext, serviceInstance, methods);
    }

    private void populateMethods(String rootCtx, Object serviceInstance, Set<Method> methods) {

        methods.stream().forEach((m) -> {
            MethodContext mc;
            mc = new MethodContext(rootCtx, serviceInstance, m);
            mc.parameters.addAll(processParameters(serviceInstance, m));

            if (m.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping rm = m.getAnnotation(RequestMapping.class);
                Arrays.asList(rm.value()).forEach(ru -> {
                    mc.requestMethods.addAll(Arrays.asList(rm.method()));
                    routes.put(rootCtx, ru, mc);

                });
            } else {
                routes.put(rootCtx, m.getName(), mc);
            }
        });
    }

    private List<MethodParam> processParameters(Object serviceInstance, Method m) {
        List<MethodParam> mps = new ArrayList<>();
        if (m.getParameterCount() > 0) {
            Arrays.asList(m.getParameters()).forEach((Parameter p) -> {
                boolean required = true;
                MethodParam<?> mp;
                if (serviceInstance.getClass().isAnnotationPresent(Service.class)) {
                    if (p.isAnnotationPresent(RequestParam.class)) {
                        mp = new MethodParam(p, MethodParam.ParameterSource.QUERY_PARAM, p.getAnnotation(RequestParam.class).value());
                        required = p.getAnnotation(RequestParam.class).required();
                        if (!Util.isEmpty(p.getAnnotation(RequestParam.class).defaultValue())) {
                            Class valueClass = p.getType();
                            mp.defaultValue = generateValueFromString(valueClass, p.getAnnotation(RequestParam.class).defaultValue());
                            mp.defaultValueClass = valueClass;
                        }
                    } else if (p.isAnnotationPresent(RequestBody.class)) {
                        mp = new MethodParam(p, MethodParam.ParameterSource.BODY, p.getName());
                        required = p.getAnnotation(RequestBody.class).required();
                    } else if (p.isAnnotationPresent(PathVariable.class)) {
                        mp = new MethodParam(p, MethodParam.ParameterSource.PATH_VARIABLE, p.getAnnotation(PathVariable.class).value());
                    } else if (p.isAnnotationPresent(RequestProperty.class)) {
                        mp = new MethodParam(p, MethodParam.ParameterSource.HEADER_PARAM, p.getAnnotation(RequestProperty.class).value());
                        required = p.getAnnotation(RequestProperty.class).required();
                    } else if (AbstractMessage.class.isAssignableFrom(p.getType())) {
                        mp = new MethodParam(p, MethodParam.ParameterSource.BODY, p.getName());
                    } else {
                        mp = new MethodParam(p, MethodParam.ParameterSource.QUERY_PARAM, p.getName());
                    }
                } else if (p.isAnnotationPresent(RequestBody.class)) {
                    mp = new MethodParam(p, MethodParam.ParameterSource.BODY, p.getName());
                    required = p.getAnnotation(RequestBody.class).required();
                } else {
                    mp = new MethodParam<>(p, MethodParam.ParameterSource.PARAMETER_WRAPPER, p.getName());
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

    private Object generateValueFromString(Class type, String sValue) {
        if (sValue == null || Util.isEmpty(sValue)) {
            return null;
        }
        //todo: find and test other primitives and base types
        if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
            return Integer.parseInt(sValue);
        } else if (Long.class.isAssignableFrom(type) || long.class.isAssignableFrom(type)) {
            return Long.parseLong(sValue);
        } else if (String.class.isAssignableFrom(type)) {
            return sValue;
        }
        return null;
    }

    class MethodContext {

        String rootCtx;
        Method method;
        Object serviceInstance;
        List<RequestMethod> requestMethods = new ArrayList<>();
        List<MethodParam> parameters = new ArrayList<>();

        Optional<MethodParam> getMethodParamForRequestBody() {
            if (parameters.isEmpty()) {
                return Optional.empty();
            }
            return parameters.stream().filter(p -> (p.source == MethodParam.ParameterSource.BODY || p.source == MethodParam.ParameterSource.PARAMETER_WRAPPER)).findAny();
        }

        private Object[] extractInvocationArguments(Request.RequestHeader header, Object messageBody) throws MissingParameterException {
            final List<Object> methodParams = new ArrayList<>();

            for (MethodParam p : parameters) {
                switch (p.source) {
                    case BODY: {
                        if (messageBody != null) {
                            methodParams.add(messageBody);
                        } else if (p.defaultValue != null) {
                            methodParams.add(p.defaultValue);
                        } else if (!p.required) {
                            methodParams.add(null);
                        } else {
                            throw new MissingParameterException(header.getUri().getPath(), p.paramId);
                        }
                        break;
                    }
                    case PARAMETER_WRAPPER: {
                        if (messageBody == null || !(messageBody instanceof ParameterWrapper)) {
                            throw new MissingParameterException(header.getUri().getPath(), p.paramId);
                        }
                        Optional<org.thingsplode.synapse.core.domain.ParameterWrapper.Parameter> pOpt = ((ParameterWrapper) messageBody).getParameterByName(p.paramId);
                        if (pOpt.isPresent()) {
                            methodParams.add(pOpt.get().getValue());
                        }
                        break;
                    }
                    case QUERY_PARAM: {
                        Object value = generateValueFromString(p.parameter.getType(), header.getUri().getQueryParamterValue(p.paramId));
                        if (value == null && p.required) {
                            throw new MissingParameterException(header.getUri().getQuery(), p.paramId);
                        }
                        methodParams.add(value);
                        break;
                    }
                    case HEADER_PARAM: {
                        Optional<String> headerValueOpt = header.getMessageProperty(p.paramId);
                        if (!headerValueOpt.isPresent() && p.required) {
                            throw new MissingParameterException("Header Value", p.paramId);
                        } else {
                            methodParams.add(headerValueOpt.get());
                        }
                        break;
                    }
                    case PATH_VARIABLE: {
                        Matcher m = p.pathVariableMatcher.matcher(header.getUri().getPath());
                        if (m.find()) {
                            methodParams.add(generateValueFromString(p.parameter.getType(), m.group()));
                        } else if (!p.required) {
                            methodParams.add(null);
                        } else {
                            throw new MissingParameterException(header.getUri().getPath(), p.paramId);
                        }
                        break;
                    }
                }
            }
            return methodParams.toArray();
        }

        String createParameterExpression() {
            if (parameters.isEmpty()) {
                return "";
            }
            String exp = "";
            boolean first = true;
            for (MethodParam p : parameters) {
                boolean skip = p.parameter.isAnnotationPresent(PathVariable.class) || p.source == MethodParam.ParameterSource.BODY || p.source == MethodParam.ParameterSource.PARAMETER_WRAPPER || p.source == MethodParam.ParameterSource.HEADER_PARAM;
                if (!skip) {
                    String pid = p.required ? p.paramId : "[" + p.paramId + "]";
                    exp = exp + (first ? "?" + pid : "&" + pid);
                    first = false;
                }
            }
            return exp;
        }

        private void preparePathVariableMatching(String methodName) {
            this.parameters.forEach(p -> {
                //check for path variables on the root context or on the request map
                if (p.source == MethodParam.ParameterSource.PATH_VARIABLE) {
                    String pathVariableName = p.parameter.getAnnotation(PathVariable.class).value();
                    Pattern regexP = Pattern.compile("\\{" + pathVariableName + "\\}");
                    if (regexP.matcher(rootCtx).find()) {
                        String pattern = "(?!\\" + rootCtx.substring(0, rootCtx.indexOf("{")) + ")" + "([A-Za-z0-9._@]+)(?=" + rootCtx.substring(rootCtx.indexOf("}") + 1, rootCtx.length()) + ")";
                        p.pathVariableMatcher = Pattern.compile(pattern);
                        //(?!/test/)([A-Za-z0-9._@]+)(?=/messages/sum)
                        p.pathVariableOnRootContext = true;
                    } else if (regexP.matcher(methodName).find()) {
                        //String pattern = (methodName.indexOf("{") == 0 ? "^" : "(?!" + methodName.substring(0, methodName.indexOf("{")) + ")") + "([A-Za-z0-9._@]+)" + (methodName.indexOf("}") == methodName.length()-1 ? "$" : "(?=" + methodName.substring(methodName.indexOf("}") + 1, methodName.length()) + ")");
                        String pattern = (methodName.indexOf("{") == 0 ? "" : "(?!" + methodName.substring(0, methodName.indexOf("{")) + ")") + "([A-Za-z0-9._@]+)" + (methodName.indexOf("}") == methodName.length() - 1 ? "$" : "(?=" + methodName.substring(methodName.indexOf("}") + 1, methodName.length()) + ")");
                        //(?!switches/)([A-Za-z0-9._@]+)$
                        //^(?!switches/)([A-Za-z0-9._@]+)$
                        p.pathVariableMatcher = Pattern.compile(pattern);
                        p.pathVariableOnRootContext = false;
                    } else {
                        throw new IllegalArgumentException("The parameter " + p.parameter.getName() + " on method " + this.method.getName() + " on class " + this.method.getDeclaringClass().getSimpleName() + " has the " + PathVariable.class.getSimpleName() + ", but no path variable defined in the root context.");
                    }
                }
            });
        }

        public MethodContext(String rootCtx, Object service, Method method) {
            this.rootCtx = rootCtx;
            this.serviceInstance = service;
            this.method = method;
            this.requestMethods = new ArrayList<>();
        }

        public MethodContext(String rootCtx, Object service, Method method, List<RequestMethod> requestMethods) {
            if (requestMethods != null) {
                this.rootCtx = rootCtx;
                this.serviceInstance = service;
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

    class Routes {

        Map<String, SortedMap<String, MethodContext>> rootCtxes = new HashMap<>();
        SortedSet<Pattern> patterns = new TreeSet<>(new PatternComparator());
        Set<String> paths = new HashSet<>();

        synchronized void put(String rootCtx, String methodName, MethodContext mc) {

            String rootRegexp = rootCtx.replaceAll("/\\{.*\\}/", "/[^/]+/");
            methodName = methodName.startsWith("/") ? methodName.substring(1) : methodName;
            methodName = methodName + mc.createParameterExpression();
            mc.preparePathVariableMatching(methodName);

            SortedMap<String, MethodContext> rootCtxMethods = rootCtxes.get(rootRegexp);
            if (rootCtxMethods != null) {
                rootCtxMethods.put(methodName, mc);
            } else {
                patterns.add(Pattern.compile(rootRegexp, Pattern.CASE_INSENSITIVE));
                SortedMap<String, MethodContext> map = new TreeMap<>();
                map.put(methodName, mc);
                rootCtxes.put(rootRegexp, map);
            }
            paths.add(rootCtx + "/" + methodName);
        }
    }

    private class PatternComparator implements Comparator<Pattern> {

        @Override
        public int compare(Pattern x, Pattern y) {
            if (x == null || y == null) {
                throw new NullPointerException("None of the compared values is accepted to be null.");
            }
            if (x.equals(y)) {
                return 0;
            } else {
                return y.pattern().length() - x.pattern().length();
            }
        }
    }

}
