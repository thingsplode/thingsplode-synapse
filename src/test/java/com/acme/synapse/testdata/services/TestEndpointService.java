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
package com.acme.synapse.testdata.services;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.acme.synapse.testdata.services.core.Filter;
import org.thingsplode.synapse.core.annotations.RequestMapping;
import org.thingsplode.synapse.core.annotations.RequestParam;
import org.thingsplode.synapse.core.annotations.Service;
import org.thingsplode.synapse.core.domain.RequestMethod;

/**
 *
 * @author tamas.csaba@gmail.com
 */
@Service("/test/first")
public class TestEndpointService implements TestEndpoint {

    @Override
    @RequestMapping("/ping")
    public void ping() {
        try {
            Thread.sleep(2000);
            System.out.println("ping executed");
        } catch (InterruptedException ex) {
            Logger.getLogger(TestEndpointService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    @RequestMapping(value = "/echo", method = {RequestMethod.GET, RequestMethod.POST})
    public String echo(@RequestParam(value = "message", required = true, defaultValue = "Hello World") String message) {
        return message;
    }

    //todo: make and test required false tests
    
    @Override
    @RequestMapping(value = "/info", method = {RequestMethod.GET})
    public Serializable getInfo() {
        return new Filter("some query", 10, 100);
    }

    @Override
    public Serializable filter(Filter filter) {
        filter.setPage(100);
        filter.setPageSize(200);
        return filter;
    }

}
