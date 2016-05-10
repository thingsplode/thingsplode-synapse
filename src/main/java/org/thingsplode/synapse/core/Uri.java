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
package org.thingsplode.synapse.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.thingsplode.synapse.util.Util;

/**
 * Processes URIs in the following way:
 * /context/service/method/{path_variable}?parameter=1&parameter=2
 * 
 * @author Csaba Tamas
 */
//todo: merge&cobine with the java.net.URI
public class Uri {

    private final String path;
    private String query;
    private List<Parameter<String>> queryParameters;

    public String createParameterExpression() {
        if (queryParameters == null || queryParameters.isEmpty()) {
            return "";
        }
        String pex = "?";
        boolean first = true;
        for (Parameter p : queryParameters) {
            pex = pex + (first ? p.getName() : "&" + p.getName());
            first = false;
        }
        return pex;
    }

    public String getQueryParamterValue(String parameterName) {
        if (queryParameters == null || queryParameters.isEmpty()) {
            return null;
        }
        Optional<Parameter<String>> o = queryParameters.stream().filter((p) -> p.getName().equalsIgnoreCase(parameterName)).findFirst();
        return o.isPresent() ? o.get().getValue() : null;
    }

    @JsonCreator
    private Uri(@JsonProperty("path") String path, @JsonProperty("query") String query, @JsonProperty("queryParameters") List<Parameter<String>> queryParameters) {
        this.path = path;
        this.query = query;
        this.queryParameters = queryParameters;
    }

    public Uri(String uri) throws UnsupportedEncodingException {
        this(uri, "UTF-8");
    }

    public Uri(String uri, String encoding) throws UnsupportedEncodingException {
        if (Util.isEmpty(uri)) {
            throw new IllegalArgumentException("The URI cannot be null.");
        }
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        int q = uri.lastIndexOf('?');
        if (q != -1) {
            query = uri.substring(q + 1);
            path = uri.substring(0, q);
        } else {
            path = uri;
        }
        if (!Util.isEmpty(query)) {
            processQuery(encoding);
        }
    }

    private void processQuery(String encoding) throws UnsupportedEncodingException {
        queryParameters = new ArrayList<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryParameters.add(new Parameter<>(URLDecoder.decode(pair.substring(0, idx), encoding), URLDecoder.decode(pair.substring(idx + 1), encoding)));
        }
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }

    public List<Parameter<String>> getQueryParameters() {
        return queryParameters;
    }

    @Override
    public String toString() {
        return "Uri{" + "path=" + path + ", query=" + query + ", queryParameters=" + queryParameters + '}';
    }

}
