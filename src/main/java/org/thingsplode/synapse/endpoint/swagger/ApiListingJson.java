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
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.SynapseReader;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.ReaderConfig;
import io.swagger.jaxrs.config.SynapseBeanConfig;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Swagger;
import io.swagger.util.Yaml;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.annotations.RequestMapping;
import org.thingsplode.synapse.core.annotations.Service;
import org.thingsplode.synapse.core.domain.MediaType;
import org.thingsplode.synapse.core.domain.Response;

/**
 *
 * @author tamas.csaba@gmail.com
 */
@Service("/swagger")
public class ApiListingJson {

    private final Logger logger = LoggerFactory.getLogger(ApiListingJson.class);
    static boolean initialized = false;
    static Swagger swaggerModel;

    public ApiListingJson() {
    }

    protected synchronized void scan() {
        BeanConfig bcScanner = new SynapseBeanConfig();
        bcScanner.setResourcePackage("com.acme"); //use comma separated list
        bcScanner.setVersion("1.0.0");
        bcScanner.setHost("loclahost:8080");
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

    @RequestMapping("/json")
    public Response getListingJson() {
        Template t = new Template() {
            @Override
            void addBody(Response response) throws JsonProcessingException {
                response.getHeader().setContentType(new MediaType("application/json"));
                response.setBody(swaggerModel);
            }
        };
        return t.execute();
    }

    @RequestMapping("/yaml")
    public Response getListingYaml() {
        Template t = new Template() {
            @Override
            void addBody(Response response) throws JsonProcessingException {
                response.getHeader().setContentType(new MediaType("text/plain"));
                response.setBody(Yaml.mapper().writeValueAsString(swaggerModel));
            }
        };
        return t.execute();
    }

}
