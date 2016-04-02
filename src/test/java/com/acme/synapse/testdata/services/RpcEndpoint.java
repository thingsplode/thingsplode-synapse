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

import com.acme.synapse.testdata.services.core.Filter;
import java.io.Serializable;
import org.thingsplode.synapse.core.SynapseEndpointServiceMarker;
import org.thingsplode.synapse.core.exceptions.MethodNotFoundException;

/**
 *
 * @author Csaba Tamas
 */
public interface RpcEndpoint extends SynapseEndpointServiceMarker {

    public void ping();

    public String echo(String message);

    public Serializable getInfo() throws MethodNotFoundException;
    public Serializable filter(Filter filter);

}
