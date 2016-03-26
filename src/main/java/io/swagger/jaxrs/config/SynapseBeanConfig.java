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
package io.swagger.jaxrs.config;

import io.swagger.annotations.SwaggerDefinition;
import java.util.HashSet;
import java.util.Set;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.thingsplode.synapse.core.SynapseEndpointServiceMarker;
import org.thingsplode.synapse.core.annotations.Service;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class SynapseBeanConfig extends BeanConfig {

    @Override
    public Set<Class<?>> classes() {
        ConfigurationBuilder config = new ConfigurationBuilder();
        Set<String> acceptablePackages = new HashSet<>();

        boolean allowAllPackages = false;

        if (resourcePackage != null && !"".equals(resourcePackage)) {
            String[] parts = resourcePackage.split(",");
            for (String pkg : parts) {
                if (!"".equals(pkg)) {
                    acceptablePackages.add(pkg);
                    config.addUrls(ClasspathHelper.forPackage(pkg));
                }
            }
        } else {
            allowAllPackages = true;
        }

        config.setScanners(new ResourcesScanner(), new TypeAnnotationsScanner(), new SubTypesScanner());

        final Reflections reflections = new Reflections(config);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Service.class);
        classes.addAll(reflections.getSubTypesOf(SynapseEndpointServiceMarker.class));
        classes.addAll(reflections.getTypesAnnotatedWith(SwaggerDefinition.class));

        Set<Class<?>> output = new HashSet<>();
        for (Class<?> cls : classes) {
            if (allowAllPackages) {
                output.add(cls);
            } else {
                acceptablePackages.stream().filter((pkg) -> (cls.getPackage().getName().startsWith(pkg))).forEach((_item) -> {
                    output.add(cls);
                });
            }
        }
        return output;
    }

}
