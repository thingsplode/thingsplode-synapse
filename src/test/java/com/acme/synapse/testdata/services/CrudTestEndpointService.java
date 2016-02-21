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

import com.acme.synapse.testdata.services.core.Device;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.ArrayList;
import org.thingsplode.synapse.core.annotations.PathVariable;
import org.thingsplode.synapse.core.annotations.RequestBody;
import org.thingsplode.synapse.core.annotations.RequestMapping;
import org.thingsplode.synapse.core.annotations.Service;
import org.thingsplode.synapse.core.domain.RequestMethod;
import org.thingsplode.synapse.core.domain.Response;

/**
 *
 * @author tamas.csaba@gmail.com
 */
@Service("/{userid}/devices/")
public class CrudTestEndpointService {

    public Response<ArrayList<Device>> listAll() {
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), new ArrayList());
    }

    @RequestMapping(value = "{deviceId}", method = {RequestMethod.GET})
    public Device getDeviceById(@PathVariable("deviceId") long id) {
        return createDevice();
    }

    //example: "/test/devices/getById/1122321
    public Response<Device> getById(@RequestBody Long deviceID) {
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), createDevice());
    }

    @RequestMapping("owner")
    public Response<Device> getByOwner() {
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), createDevice());
    }

    @RequestMapping("owner")
    public Response<Device> getByOwner(Long ownerId) {
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), createDevice());
    }
    
    @RequestMapping("owner")
    public Response<Device> getByOwner(Long ownerId, Integer limit) {
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), createDevice());
    }

    @RequestMapping("owner/old")
    public Response<Device> getByOwnerOld() {
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), createDevice());
    }

    @RequestMapping(value = "add", method = {RequestMethod.POST, RequestMethod.PUT})
    public Device createDevice() {
        Device d = new Device();
        d.setLogicalName("some device");
        ArrayList sdevices = new ArrayList<>();
        sdevices.add(d);
        d.setSubDevices(sdevices);
        return d;
    }
    //public Response<> delete()

    /**
     * Use cases: - no method definition (GE, DELETE, etc.) - delete crud
     * service is exsiting
     */
}
