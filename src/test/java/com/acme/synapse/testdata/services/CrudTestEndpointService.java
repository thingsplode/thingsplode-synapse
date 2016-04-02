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

import com.acme.synapse.testdata.services.core.Address;
import com.acme.synapse.testdata.services.core.Device;
import com.acme.synapse.testdata.services.core.Tuple;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.Calendar;
import javax.ws.rs.QueryParam;
import org.thingsplode.synapse.core.annotations.PathVariable;
import org.thingsplode.synapse.core.annotations.RequestBody;
import org.thingsplode.synapse.core.annotations.RequestMapping;
import org.thingsplode.synapse.core.annotations.Service;
import org.thingsplode.synapse.core.domain.Request.RequestHeader.RequestMethod;
import org.thingsplode.synapse.core.domain.Response;

/**
 *
 * @author Csaba Tamas
 */
@Service("/{userid}/devices/")
@Api(consumes = "application/json",produces = "application/json")
public class CrudTestEndpointService {

    public Response<ArrayList<Device>> listAll() {
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), new ArrayList());
    }

    @RequestMapping(value = "{deviceId}", method = {RequestMethod.GET})
    public Device getDeviceById(@PathVariable("deviceId") long id) {
        Device d = createDevice();
        d.setId(id);
        d.setAddress(new Address("some street", "some country", 4040));
        d.setServicePeriod(new Tuple<>(Calendar.getInstance().getTime(), Calendar.getInstance().getTime()));
        d.setTreshold(100);
        return d;
    }

    @RequestMapping(value = "/switches/{deviceId}", method = {RequestMethod.GET})
    public Device getSwicthesById(@PathVariable("deviceId") long id) {
        Device d = createDevice();
        d.setId(id);
        return d;
    }

    //example: "/test/devices/getById/1122321
    public Response<Device> getById(@RequestBody Long deviceID) {
        Device d = createDevice();
        d.setId(deviceID);
        d.setTreshold(100);
        d.setServicePeriod(new Tuple<>(Calendar.getInstance().getTime(), Calendar.getInstance().getTime()));
        d.setAddress(new Address("some street", "some country", 4040));
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), d);
    }

    @RequestMapping(value = "owner", method = {RequestMethod.GET})
    public Response<Device> getByOwner() {
        Device d = createDevice();
        d.setLogicalName("owner");
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), d);
    }

    @RequestMapping(value = "owner", method = {RequestMethod.POST})
    public Response<Device> getByOwner(@QueryParam("arg0") Long deviceId) {
        Device d = createDevice();
        d.setId(deviceId);
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), d);
    }

    @RequestMapping("owner")
    public Response<Device> getByOwner(@QueryParam("arg0") Long deviceId,@QueryParam("arg1") Integer treshold) {
        Device d = createDevice();
        d.setId(deviceId);
        d.setTreshold(treshold);
        return new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), d);
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
        sdevices.add(new Device(1124, "sub device", Integer.SIZE));
        d.setSubDevices(sdevices);
        return d;
    }
    //public Response<> delete()

    /**
     * Use cases: - no method definition (GE, DELETE, etc.) - delete crud
     * service is exsiting
     */
}
