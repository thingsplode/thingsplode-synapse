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
import org.thingsplode.synapse.core.exceptions.MethodNotFoundException;

/**
 *
 * @author Csaba Tamas
 */
public class RpcEndpointImpl implements RpcEndpoint {

    @Override
    public void ping() {
        try {
            Thread.sleep(2000);
            System.out.println("ping executed");
        } catch (InterruptedException ex) {
            Logger.getLogger(RpcEndpointImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void pingWithTimeout(int timeout) {
        try {
            Thread.sleep(timeout);
            System.out.println("ping executed");
        } catch (InterruptedException ex) {
            Logger.getLogger(RpcEndpointImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String echo(String message) {
        return message;
    }

    @Override
    public String echoWithTimeout(long timeout, String id) throws InterruptedException {
        Thread.sleep(timeout);
        return id;
    }

    //todo: make and test required false tests
    @Override
    public Serializable getInfo() throws MethodNotFoundException {
        return new Filter("some query", 10, 100);
    }

    @Override
    public Serializable filter(Filter filter) {
        filter.setPage(100);
        filter.setPageSize(200);
        return filter;
    }

}
