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
package org.thingsplode.synapse.proxy;

import org.thingsplode.synapse.core.domain.Event;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class EndpointProxy {

    private EndpointProxy() {
    }

    public static final EndpointProxy init() {
        return new EndpointProxy();
    }

    public <T> T createStub(String test_service, Class<T> aClass) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
    public void broadcast(Event event){
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
    public Response dispatch(Request request){
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
    
}
