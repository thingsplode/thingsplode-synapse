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
package org.thingsplode.synapse.endpoint.swagger;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.swagger.config.SwaggerConfig;
import io.swagger.jaxrs.SynapseReader;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.ReaderConfig;
import io.swagger.jaxrs.config.SynapseBeanConfig;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Swagger;
import io.swagger.util.Yaml;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.annotations.RequestMapping;
import org.thingsplode.synapse.core.annotations.Service;
import org.thingsplode.synapse.core.domain.MediaType;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.endpoint.Endpoint;
import org.thingsplode.synapse.util.Util;
import org.thingsplode.synapse.core.annotations.RequestProperty;

/**
 * A swagger scanner to list the endpoint information;
 *
 * @author Csaba Tamas
 */
@Service(Endpoint.ENDPOINT_URL_PATERN)
public class EndpointApiGenerator {

    private final Logger logger = LoggerFactory.getLogger(EndpointApiGenerator.class);
    static boolean initialized = false;
    static Swagger swaggerModel;
    final private Set<String> packages = new HashSet<>();
    final private String apiVersion;
    final private String host;

    /**
     *
     * @param apiVersion the API version
     * @param host the host to which the endpoints are exposed
     */
    public EndpointApiGenerator(String apiVersion, String host) {
        this.apiVersion = apiVersion;
        this.host = host;
    }

    protected synchronized void scan() {
        BeanConfig bcScanner = new SynapseBeanConfig();
        //todo: read more content from the META-INF/manifest and add it to the service/endpoint description
        bcScanner.setResourcePackage(this.packages.size() > 0 ? this.packages.stream().reduce((base, elm) -> base + "," + elm).get() : "."); //generate comma separated list
        bcScanner.setVersion(this.apiVersion);
        bcScanner.setHost(this.host);
        bcScanner.setBasePath("/");
        bcScanner.setScan(true);

        //ScannerFactory.getScanner();
        logger.debug("using scanner " + bcScanner);
        SwaggerSerializers.setPrettyPrint(bcScanner.getPrettyPrint());
        Set<Class<?>> classes = bcScanner.classes();
        if (classes != null) {
            SynapseReader reader = new SynapseReader(swaggerModel, new ReaderConfig() {
                @Override
                public boolean isScanAllResources() {
                    return true;
                }

                @Override
                public Collection<String> getIgnoredRoutes() {
                    return Collections.EMPTY_LIST;
                }
            });
            swaggerModel = reader.read(classes);
            if (bcScanner instanceof SwaggerConfig) {
                swaggerModel = ((SwaggerConfig) bcScanner).configure(swaggerModel);
            }
        }

        initialized = true;
    }

    abstract class Template {

        Response execute() {
            if (!initialized) {
                scan();
            }
            if (swaggerModel != null) {
                try {
                    Response.ResponseHeader h = new Response.ResponseHeader(HttpResponseStatus.OK);
                    Response response = new Response(h);
                    addBody(response);
                    return response;
                } catch (JsonProcessingException ex) {
                    return new Response(new Response.ResponseHeader(HttpResponseStatus.METHOD_NOT_ALLOWED), ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
            } else {
                return new Response(new Response.ResponseHeader(HttpResponseStatus.METHOD_NOT_ALLOWED), "Swagger scanner is not set");
            }
        }

        abstract void addBody(Response response) throws JsonProcessingException;
    }

    public void addPackageToBeScanned(String pack) {
        this.packages.add(pack);
    }

    @RequestMapping("/json")
    public Response getListingJson(@RequestProperty("Host") String host) {
        Template t = new Template() {
            @Override
            void addBody(Response response) throws JsonProcessingException {
                response.getHeader().setContentType(new MediaType("application/json"));
                if (!Util.isEmpty(host)) {
                    //todo: this method overwrites the original host value (which might not be a big problem)
                    swaggerModel.setHost(host);
                }
                response.setBody(swaggerModel);
            }
        };
        return t.execute();
    }

    @RequestMapping("/yaml")
    public Response getListingYaml(@RequestProperty("Host") String host) {
        Template t = new Template() {
            @Override
            void addBody(Response response) throws JsonProcessingException {
                response.getHeader().setContentType(new MediaType("text/plain"));
                if (!Util.isEmpty(host)) {
                    //todo: this method overwrites the original host value (which might not be a big problem)
                    swaggerModel.setHost(host);
                }
                response.setBody(Yaml.mapper().writeValueAsString(swaggerModel));
            }
        };
        return t.execute();
    }

}
