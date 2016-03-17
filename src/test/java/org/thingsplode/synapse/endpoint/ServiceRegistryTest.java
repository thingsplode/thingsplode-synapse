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
package org.thingsplode.synapse.endpoint;

import com.acme.synapse.testdata.services.CrudTestEndpointService;
import com.acme.synapse.testdata.services.DummyMarkedEndpoint;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.acme.synapse.testdata.services.RpcEndpointImpl;
import com.acme.synapse.testdata.services.EndpointTesterService;
import com.acme.synapse.testdata.services.core.Address;
import com.acme.synapse.testdata.services.core.Device;
import com.acme.synapse.testdata.services.core.Filter;
import com.acme.synapse.testdata.services.core.Tuple;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.Assert;
import org.thingsplode.synapse.core.domain.Uri;
import org.thingsplode.synapse.core.domain.ParameterWrapper;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Request.RequestHeader.RequestMethod;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.exceptions.ExecutionException;
import org.thingsplode.synapse.core.exceptions.MarshallerException;
import org.thingsplode.synapse.core.exceptions.MethodNotFoundException;
import org.thingsplode.synapse.core.exceptions.MissingParameterException;
import org.thingsplode.synapse.core.exceptions.SerializationException;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class ServiceRegistryTest {

    private final ServiceRegistry registry = new ServiceRegistry();
    private boolean inited = false;

    public ServiceRegistryTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        if (!inited) {
            registry.register(null, new RpcEndpointImpl());
            registry.register(null, new DummyMarkedEndpoint());
            registry.register(null, new EndpointTesterService());
            registry.register(null, new CrudTestEndpointService());
            inited = true;
        }
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testRequestMethodDispatching() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, MarshallerException, SerializationException {
        ///test request method matching
        Optional<ServiceRegistry.MethodContext> optM = registry.getMethodContext(new Request.RequestHeader(UUID.randomUUID().toString(), new Uri("/1221221/devices/add"), RequestMethod.GET));
        Assert.assertTrue(!optM.isPresent());

        Optional<ServiceRegistry.MethodContext> optM1 = registry.getMethodContext(new Request.RequestHeader(UUID.randomUUID().toString(), new Uri("/1221221/devices/add"), RequestMethod.POST));
        Assert.assertTrue(optM1.isPresent());

        Optional<ServiceRegistry.MethodContext> optM2 = registry.getMethodContext(new Request.RequestHeader(UUID.randomUUID().toString(), new Uri("/1221221/devices/add"), RequestMethod.PUT));
        Assert.assertTrue(optM2.isPresent());
        ///----------------------------
        assertResponse(registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/1221221/devices/add"), RequestMethod.POST), null));
        assertResponse(registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/1221221/devices/add"), RequestMethod.PUT), null));
    }
    
    private void assertResponse(Response<Device> rsp) {
        Assert.assertTrue(rsp != null);
        Assert.assertTrue(rsp.getBody() instanceof Device);
        Assert.assertEquals("some device", rsp.getBody().getLogicalName());
        Assert.assertTrue(rsp.getBody().getSubDevices().size() == 1);

    }

    @Test
    public void testMultipleUrisSameMethod() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, SerializationException {
        ///Test multiple request uris to the same method and named parameters
        Optional<ServiceRegistry.MethodContext> opt = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/test/user.name/messages/calculate?a=2&b=3"), RequestMethod.GET));
        Assert.assertTrue(opt.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt.get().rootCtx);

        Optional<ServiceRegistry.MethodContext> opt1 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/test/user.name/messages/add?a=1231434&b=212112"), RequestMethod.GET));
        Assert.assertTrue(opt1.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt1.get().rootCtx);
        ///----------------------------
        Response<Integer> rsp = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/test/user.name/messages/calculate?a=2&b=3"), RequestMethod.GET), null);
        Assert.assertTrue(rsp.getBody() == 5);

        Response<Integer> rsp2 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/test/user.name/messages/add?a=1231434&b=212112"), RequestMethod.GET), null);
        Assert.assertTrue(rsp2.getBody() == 1443546);
    }

    @Test
    public void testRequestBody() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, SerializationException {
        //Optional<ServiceRegistry.MethodContext> opt = registry.getMethodContext(RequestMethod.GET, new Uri("/test/user.name/messages/check_address"));
        Optional<ServiceRegistry.MethodContext> opt = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/test/user.name/messages/check_address"), RequestMethod.GET));
        Assert.assertTrue(opt.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt.get().rootCtx);

        final String country = "some country";
        final String street = "some street";
        Response<Address> rsp1 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/test/user.name/messages/check_address"), RequestMethod.GET), new Address(street, country, 4040));
        Assert.assertTrue(rsp1.getBody().getCountry().equalsIgnoreCase(country));
        Assert.assertTrue(rsp1.getBody().getStreet().equalsIgnoreCase(street));
        Assert.assertTrue(rsp1.getBody().getPostalCode() == 5050);

        Optional<ServiceRegistry.MethodContext> opt2 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/user.name/devices/getById"), RequestMethod.GET));
        Assert.assertTrue(opt2.isPresent());
        Assert.assertEquals("/{userid}/devices", opt2.get().rootCtx);

        Response<Device> rsp2 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/user.name/devices/getById"), RequestMethod.GET), new Long(112));
        Assert.assertTrue(rsp2.getBody().getId() == 112);

    }

    //@Test()
    public void testMissingParams() throws MethodNotFoundException, ExecutionException, MissingParameterException, UnsupportedEncodingException, SerializationException {
        //todo: reenable this
        Response<Integer> rsp1 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/test/user.name/messages/add"), RequestMethod.GET), null);
        Response<Integer> rsp2 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/test/user.name/messages/add?a=1231434"), RequestMethod.GET), null);
        Response<Integer> rsp3 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/test/user.name/messages/add?a=1231434&b="), RequestMethod.GET), null);
        Response<Integer> rsp4 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/test/user.name/messages/add?a=1231434&b=null"), RequestMethod.GET), null);
        Assert.assertTrue(rsp1.getBody() == 1443546);
    }

    @Test
    public void testPathVariableResolution1() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, SerializationException {
        ///Test path variable resolution
        Optional<ServiceRegistry.MethodContext> opt2 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/test/user.name/messages/clear"), RequestMethod.GET));
        Assert.assertTrue(opt2.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt2.get().rootCtx);

        //(?!/test/)([.a-z0-9]+)(?=/messages/clear)
        //([A-Za-z0-9._@]+)$
        Response<Integer> rsp = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/test/user.name/messages/clear"), RequestMethod.GET), null);
        Assert.assertTrue(rsp.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp.getBody() == null);
    }

    @Test
    public void testPathVariableResolution2() throws MethodNotFoundException, ExecutionException, MissingParameterException, UnsupportedEncodingException, SerializationException {
        Optional<ServiceRegistry.MethodContext> optGbdi = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/1212212/devices/2323434"), RequestMethod.GET));
        Assert.assertTrue(optGbdi.isPresent());
        //(?!/)([.a-z0-9]+)(?=/devices/)
        Response<Device> rsp = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/1212212/devices/2323434"), RequestMethod.GET), null);
        Assert.assertTrue(rsp.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp.getBody().getId() == 2323434l);

        Optional<ServiceRegistry.MethodContext> optGbdi1 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/1212212/devices/switches/1998"), RequestMethod.GET));
        Assert.assertTrue(optGbdi1.isPresent());
        Response<Device> rsp1 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/1212212/devices/switches/1998"), RequestMethod.GET), null);
        Optional<ServiceRegistry.MethodContext> opt1 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/test/user.name/messages/add?a=1231434&b=212112"), RequestMethod.GET));
        Assert.assertTrue(rsp1.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp1.getBody().getId() == 1998);
    }

    @Test
    public void testPathVariableResolution3() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, SerializationException {
        Optional<ServiceRegistry.MethodContext> optGbdi2 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/1212212/devices/getById/1122321"), RequestMethod.GET));
        Assert.assertTrue(optGbdi2.isPresent());
        Response<Device> rsp = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/1212212/devices/getById/1122321"), RequestMethod.GET), null);
        Assert.assertTrue(rsp.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp.getBody().getId() == 1122321l);
    }

    @Test(expected = MethodNotFoundException.class)
    public void testNotImplemented() throws MethodNotFoundException, ExecutionException, MissingParameterException, UnsupportedEncodingException, SerializationException {

        Optional<ServiceRegistry.MethodContext> opt3 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/test/user/name/messages/clear"), RequestMethod.GET));
        Assert.assertTrue(!opt3.isPresent());

        Response rspX = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/test/user/name/messages/clear"), RequestMethod.GET), null);
        Optional<ServiceRegistry.MethodContext> opt1 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/test/user.name/messages/add?a=1231434&b=212112"), RequestMethod.GET));
        Assert.assertTrue(rspX.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rspX.getBody() == null);
    }

    @Test
    public void testOverloadedMethods() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, SerializationException {
        ///Test overloaded methods
        Optional<ServiceRegistry.MethodContext> elOpt1 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/222/devices/owner"), RequestMethod.GET));
        Assert.assertTrue(elOpt1.isPresent());

        Response<Device> rsp1 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/222/devices/owner"), RequestMethod.GET), null);
        Assert.assertTrue(rsp1.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp1.getBody().getLogicalName().equalsIgnoreCase("owner"));

        Optional<ServiceRegistry.MethodContext> elOpt2 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/222/devices/owner?arg0=222"), RequestMethod.GET));
        Assert.assertTrue(elOpt2.isPresent());

        Response<Device> rsp2 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/222/devices/owner?arg0=222"), RequestMethod.GET), null);
        Optional<ServiceRegistry.MethodContext> opt1 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/test/user.name/messages/add?a=1231434&b=212112"), RequestMethod.GET));
        Assert.assertTrue(rsp2.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp2.getBody().getLogicalName().equalsIgnoreCase("some device"));
        Assert.assertTrue(rsp2.getBody().getId() == 222);
        Assert.assertTrue(rsp2.getBody().getTreshold() == null);

        Optional<ServiceRegistry.MethodContext> elOpt3 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/222/devices/owner?arg0=222&arg1=114"), RequestMethod.GET));
        Assert.assertTrue(elOpt3.isPresent());

        Response<Device> rsp3 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/222/devices/owner?arg0=222&arg1=114"), RequestMethod.GET), null);
        Assert.assertTrue(rsp3.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp3.getBody().getLogicalName().equalsIgnoreCase("some device"));
        Assert.assertTrue(rsp3.getBody().getId() == 222);
        Assert.assertTrue(rsp3.getBody().getTreshold() == 114);

        Optional<ServiceRegistry.MethodContext> elOpt4 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/222/devices/owner/old"), RequestMethod.GET));
        Assert.assertTrue(elOpt4.isPresent());

        Response<Device> rsp4 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/222/devices/owner/old"), RequestMethod.GET), null);
        Assert.assertTrue(rsp4.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp4.getBody().getLogicalName().equalsIgnoreCase("some device"));
        ///----------------------------
    }

    @Test
    public void testReqMethodWithReqMapping() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, SerializationException {
        ///Test Request Response with request mapping
        Optional<ServiceRegistry.MethodContext> opt8 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/test/user@name/messages/sum"), RequestMethod.GET));
        Assert.assertTrue(opt8.isPresent());
        //(?!/test/)([.@a-z0-9]+)(?=/messages/sum)
        //(?!/test/)([A-Za-z0-9._@]+)(?=/messages/sum)
        Request<Tuple<Integer, Integer>> req = new Request<>(new Request.RequestHeader(null, new Uri("/test/user@name/messages/sum"), RequestMethod.fromHttpMethod(HttpMethod.GET)), new Tuple<>(10, 10));
        Response<Integer> rsp1 = registry.invokeWithJavaObject(req.getHeader(), req);
        Assert.assertTrue(rsp1.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp1.getBody() == 20);

        Request<Tuple<Integer, Integer>> reqForMultiply = new Request<>(new Request.RequestHeader(null, new Uri("/test/user@name/messages/multiply"), RequestMethod.fromHttpMethod(HttpMethod.GET)), new Tuple<>(10, 10));
        Response<Integer> multiplyRsp = registry.invokeWithJavaObject(reqForMultiply.getHeader(), reqForMultiply);
        Assert.assertTrue(multiplyRsp.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(multiplyRsp.getBody() == 100);

        
        Optional<ServiceRegistry.MethodContext> opt9 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/some_user/devices/listAll"), RequestMethod.GET));
        Assert.assertTrue(opt9.isPresent());

        Response<ArrayList<Device>> rsp2 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/some_user/devices/listAll"), RequestMethod.GET), null);
        Assert.assertTrue(rsp2.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(0 == rsp2.getBody().size());
    }

    @Test
    public void testInterfaceMarkedServices() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, SerializationException {
        ///Test interface marked services
        Optional<ServiceRegistry.MethodContext> opt4 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/com/acme/synapse/testdata/services/DummyMarkedEndpoint/"), RequestMethod.GET));
        Assert.assertTrue(!opt4.isPresent());

        Optional<ServiceRegistry.MethodContext> opt5 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/com/acme/synapse/testdata/services/DummyMarkedEndpoint/echo"), RequestMethod.GET));
        Assert.assertTrue(opt5.isPresent());

        Response<String> rsp1 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/com/acme/synapse/testdata/services/DummyMarkedEndpoint/echo"), RequestMethod.GET), ParameterWrapper.create().add("arg0", String.class, "\"something in the rain\""));
        Assert.assertTrue(rsp1.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp1.getBody().equalsIgnoreCase("Greetings earthlings: " + "\"something in the rain\""));

        Response<String> rsp2 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/com/acme/synapse/testdata/services/DummyMarkedEndpoint/echo"), RequestMethod.GET), ParameterWrapper.create().add("arg0", String.class, "something in the rain"));
        Assert.assertTrue(rsp2.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp2.getBody().equalsIgnoreCase("Greetings earthlings: " + "something in the rain"));
        ///----------------------------
    }

    @Test
    public void testRpcServices1() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, SerializationException {
        //Test RPC interfaces
        Optional<ServiceRegistry.MethodContext> opt6 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/com/acme/synapse/testdata/services/RpcEndpoint/ping"), RequestMethod.GET));
        Assert.assertTrue(!opt6.isPresent());

        Optional<ServiceRegistry.MethodContext> opt7 = registry.getMethodContext(new Request.RequestHeader(null, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/ping"), RequestMethod.GET));
        Assert.assertTrue(opt7.isPresent());

        Response rsp = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/ping"), RequestMethod.GET), null);
        Assert.assertTrue(rsp.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp.getBody() == null);
    }

    @Test
    public void testRpcServices2() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, SerializationException {
        Response<String> rsp1 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/echo"), RequestMethod.GET), ParameterWrapper.create().add("arg0", String.class, "Hello people"));
        Assert.assertTrue(rsp1.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp1.getBody().equalsIgnoreCase("Hello people"));
    }

    @Test
    public void testRpcServices3() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, SerializationException {
        Response<Filter> rsp2 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/getInfo"), RequestMethod.GET), null);
        Assert.assertTrue(rsp2.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp2.getBody().getQuery().equalsIgnoreCase("some query"));
        Assert.assertTrue(rsp2.getBody().getPage() == 10);
        Assert.assertTrue(rsp2.getBody().getPageSize() == 100);
    }

    @Test
    public void testRpcServices4() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, SerializationException {
        Response<Filter> rsp3 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/filter"), RequestMethod.GET), ParameterWrapper.create().add("arg0", Filter.class, new Filter("other query", Integer.MAX_VALUE, Integer.MAX_VALUE)));
        Assert.assertTrue(rsp3.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp3.getBody().getPage() == 100);
        Assert.assertTrue(rsp3.getBody().getPageSize() == 200);
    }

    @Test(expected = MethodNotFoundException.class)
    public void testNegative() throws MethodNotFoundException, ExecutionException, MissingParameterException, UnsupportedEncodingException, SerializationException {
        Response<Filter> rsp1 = registry.invokeWithJavaObject(new Request.RequestHeader(null, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/getInfo?arg0=some param"), RequestMethod.GET), null);
    }

}
