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
package org.thingsplode.synapse.services;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.thingsplode.synapse.core.annotations.PathVariable;
import org.thingsplode.synapse.core.annotations.RequestBody;
import org.thingsplode.synapse.core.annotations.RequestMapping;
import org.thingsplode.synapse.core.annotations.Service;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.services.domain.Address;
import org.thingsplode.synapse.services.domain.Tuple;

/**
 *
 * @author tamas.csaba@gmail.com
 */
@Service("/test/{user}/messages/")
public class TestSecondEndpointService {
    
    @RequestMapping({"add","calculate"})
    public Response<Integer> add(Request<Tuple<Integer, Integer>> req){
        Integer res = req.getBody().x + req.getBody().y;
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), res);
    }
    
    public Response<Integer> multiply(Request<Tuple<Integer, Integer>> req){
        Integer res = req.getBody().x * req.getBody().y;
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), res);
    }
    
    @RequestMapping("/clear")
    public void clearAll(@PathVariable("user") String user){
        System.out.println("Cleared");
    }
    
   public boolean verifyAddress(@RequestBody Address address){
       System.out.println("Address received");
       return true;
   }
    
}
