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
package org.thingsplode.synapse.serializers;

import org.thingsplode.synapse.serializers.SynapseSerializer;
import com.acme.synapse.testdata.services.core.Device;
import com.acme.synapse.testdata.services.core.Tuple;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;
import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.thingsplode.synapse.core.domain.ParameterWrapper;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Request.RequestHeader.RequestMethod;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.domain.Uri;
import org.thingsplode.synapse.core.exceptions.SerializationException;
import org.thingsplode.synapse.serializers.jackson.JacksonSerializer;

/**
 *
 * @author Csaba Tamas
 */
public class AbstractParserTest {
    
    private final JacksonSerializer serializer = new JacksonSerializer(true);
    
    public SynapseSerializer<String> getSerializer() {
        return serializer;
    }
    
    @BeforeClass
    public static void beforeClass() {
        BasicConfigurator.configure();
    }
    
    @Test
    public void testBaseParameterWrapper() throws SerializationException {
        String paramid = "arg0";
        String paramValue = "something in the rain";
        String json = getSerializer().marshallToWireformat(ParameterWrapper.create().add(paramid, paramValue.getClass(), paramValue));
        System.out.println(json);
        ParameterWrapper pw = getSerializer().unMarshall(ParameterWrapper.class, json);
        Assert.assertNotNull(pw);
        Assert.assertTrue(!pw.getParams().isEmpty());
        Assert.assertTrue(pw.getParams().get(0).getParamid().equals(paramid));
        Assert.assertTrue(pw.getParams().get(0).getValue().equals(paramValue));
        Assert.assertTrue(pw.getParams().get(0).getType().equals(paramValue.getClass()));
    }
    
    @Test
    public void TestComplexParameterWrapper() throws SerializationException {
        
        String paramid = "arg0";
        Device paramValue = Device.createTestDevice();
        String json = getSerializer().marshallToWireformat(ParameterWrapper.create().add(paramid, paramValue.getClass(), paramValue));
        System.out.println(json);
        ParameterWrapper pw = getSerializer().unMarshall(ParameterWrapper.class, json);
        Assert.assertNotNull(pw);
        Assert.assertTrue(!pw.getParams().isEmpty());
        Assert.assertTrue(pw.getParams().get(0).getParamid().equals(paramid));
        Assert.assertTrue(pw.getParams().get(0).getValue() instanceof Device);
        Assert.assertTrue(pw.getParams().get(0).getType().equals(paramValue.getClass()));
        
    }
    
    @Test
    public void testPrimitivemarshalling() throws SerializationException {
        String json = getSerializer().marshallToWireformat(Long.parseLong("1"));
        System.out.println(json);
        Long number = getSerializer().unMarshall(Long.class, json);
        Assert.assertNotNull(number);
        Assert.assertTrue(number == 1l);
    }
    
    @Test
    public void testMarshallingComplexObject() throws SerializationException {
        String json = getSerializer().marshallToWireformat(Device.createTestDevice());
        System.out.println(json);
        Device d = getSerializer().unMarshall(Device.class, json);
        Assert.assertNotNull(d);
        Assert.assertTrue(d.getLogicalName().equalsIgnoreCase("test device logical name"));
        Assert.assertTrue(d.getSubDevices().get(0).getLogicalName().equalsIgnoreCase("subdevice 1 logical name"));
    }
    
    @Test
    public void testMarshallingRequestObject() throws UnsupportedEncodingException, SerializationException {
        Request<Device> r = (Request<Device>) Request.create(UUID.randomUUID().toString(), new Uri("/1221221/devices/add"), Request.RequestHeader.RequestMethod.GET, Device.createTestDevice());
        String json = getSerializer().marshallToWireformat(r);
        System.out.println(json);
        Request ur = getSerializer().unMarshall(Request.class, json);
        Assert.assertNotNull(ur);
        Assert.assertTrue(ur.getHeader().getMethod() == Request.RequestHeader.RequestMethod.GET);
        Assert.assertTrue(ur.getHeader().getUri().getPath().equalsIgnoreCase("/1221221/devices/add"));
    }
    
    @Test
    public void testMarshallingResponseObjects() throws SerializationException {
        Response r = new Response<>(new Response.ResponseHeader(HttpResponseStatus.OK), new ArrayList());
        String json = getSerializer().marshallToWireformat(r);
        System.out.println(json);
        Response<ArrayList> ur = getSerializer().unMarshall(Response.class, json);
        Assert.assertNotNull(ur);
        Assert.assertTrue(ur.getHeader() != null);
        Assert.assertTrue(ur.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertNotNull(ur.getBody());
        Assert.assertTrue(ur.getBody().isEmpty());
    }
    
    @Test
    public void testMarshallingTuple() throws UnsupportedEncodingException, SerializationException{
        Request<Tuple<Integer, Integer>> reqForMultiply = new Request<>(new Request.RequestHeader(null, new Uri("/test/user@name/messages/multiply"), RequestMethod.fromHttpMethod(HttpMethod.GET)), new Tuple<>(10, 100));
        String json = getSerializer().marshallToWireformat(reqForMultiply);
        System.out.println(json);
        Request<Tuple<Integer, Integer>> req = getSerializer().unMarshall(Request.class, json);
        Assert.assertNotNull(req);
        Assert.assertTrue(req.getHeader() != null);
        Assert.assertTrue(req.getHeader().getMethod() == RequestMethod.GET);
        Assert.assertNotNull(req.getBody());
        Assert.assertTrue(req.getBody().x == 10);
        Assert.assertTrue(req.getBody().y == 100);
    }
}
