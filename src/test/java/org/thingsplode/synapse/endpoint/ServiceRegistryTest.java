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
import org.junit.Assert;
import org.thingsplode.synapse.core.Uri;
import org.thingsplode.synapse.core.domain.ParameterWrapper;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.RequestMethod;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.exceptions.ExecutionException;
import org.thingsplode.synapse.core.exceptions.MarshallerException;
import org.thingsplode.synapse.core.exceptions.MethodNotFoundException;
import org.thingsplode.synapse.core.exceptions.MissingParameterException;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class ServiceRegistryTest {

    private ServiceRegistry registry = new ServiceRegistry();
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
            registry.register(new RpcEndpointImpl());
            registry.register(new DummyMarkedEndpoint());
            registry.register(new EndpointTesterService());
            registry.register(new CrudTestEndpointService());
            inited = true;
        }
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testRequestMethodDispatching() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException, MarshallerException {
        ///test request method matching
        Optional<ServiceRegistry.MethodContext> optM = registry.getMethodContext(RequestMethod.GET, new Uri("/1221221/devices/add"));
        Assert.assertTrue(!optM.isPresent());

        Optional<ServiceRegistry.MethodContext> optM1 = registry.getMethodContext(RequestMethod.POST, new Uri("/1221221/devices/add"));
        Assert.assertTrue(optM1.isPresent());

        Optional<ServiceRegistry.MethodContext> optM2 = registry.getMethodContext(RequestMethod.PUT, new Uri("/1221221/devices/add"));
        Assert.assertTrue(optM2.isPresent());
        ///----------------------------
        assertResponse(registry.invoke(RequestMethod.POST, new Uri("/1221221/devices/add"), null));
        assertResponse(registry.invoke(RequestMethod.PUT, new Uri("/1221221/devices/add"), null));
    }

    private void assertResponse(Response<Device> rsp) {
        Assert.assertTrue(rsp != null);
        Assert.assertTrue(rsp.getBody() instanceof Device);
        Assert.assertEquals("some device", rsp.getBody().getLogicalName());
        Assert.assertTrue(rsp.getBody().getSubDevices().size() == 1);

    }

    @Test
    public void testMultipleUrisSameMethod() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException {
        ///Test multiple request uris to the same method and named parameters
        Optional<ServiceRegistry.MethodContext> opt = registry.getMethodContext(RequestMethod.GET, new Uri("/test/user.name/messages/calculate?a=2&b=3"));
        Assert.assertTrue(opt.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt.get().rootCtx);

        Optional<ServiceRegistry.MethodContext> opt1 = registry.getMethodContext(RequestMethod.GET, new Uri("/test/user.name/messages/add?a=1231434&b=212112"));
        Assert.assertTrue(opt1.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt1.get().rootCtx);
        ///----------------------------
        Response<Integer> rsp = registry.invoke(RequestMethod.GET, new Uri("/test/user.name/messages/calculate?a=2&b=3"), null);
        Assert.assertTrue(rsp.getBody() == 5);

        Response<Integer> rsp2 = registry.invoke(RequestMethod.GET, new Uri("/test/user.name/messages/add?a=1231434&b=212112"), null);
        Assert.assertTrue(rsp2.getBody() == 1443546);
    }

    @Test
    public void testRequestBody() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException {
        Optional<ServiceRegistry.MethodContext> opt = registry.getMethodContext(RequestMethod.GET, new Uri("/test/user.name/messages/check_address"));
        Assert.assertTrue(opt.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt.get().rootCtx);

        final String country = "some country";
        final String street = "some street";
        Response<Address> rsp1 = registry.invoke(RequestMethod.GET, new Uri("/test/user.name/messages/check_address"), new Address(street, country, 4040));
        Assert.assertTrue(rsp1.getBody().getCountry().equalsIgnoreCase(country));
        Assert.assertTrue(rsp1.getBody().getStreet().equalsIgnoreCase(street));
        Assert.assertTrue(rsp1.getBody().getPostalCode() == 5050);
        
        Optional<ServiceRegistry.MethodContext> opt2 = registry.getMethodContext(RequestMethod.GET, new Uri("/user.name/devices/getById"));
        Assert.assertTrue(opt2.isPresent());
        Assert.assertEquals("/{userid}/devices", opt2.get().rootCtx);
        
        Response<Device> rsp2 = registry.invoke(RequestMethod.GET, new Uri("/user.name/devices/getById"), new Long(112));
        Assert.assertTrue(rsp2.getBody().getId() == 112);

    }

    //@Test()
    public void testMissingParams() throws MethodNotFoundException, ExecutionException, MissingParameterException, UnsupportedEncodingException {
        Response<Integer> rsp1 = registry.invoke(RequestMethod.GET, new Uri("/test/user.name/messages/add"), null);
        Response<Integer> rsp2 = registry.invoke(RequestMethod.GET, new Uri("/test/user.name/messages/add?a=1231434"), null);
        Response<Integer> rsp3 = registry.invoke(RequestMethod.GET, new Uri("/test/user.name/messages/add?a=1231434&b="), null);
        Response<Integer> rsp4 = registry.invoke(RequestMethod.GET, new Uri("/test/user.name/messages/add?a=1231434&b=null"), null);
        Assert.assertTrue(rsp1.getBody() == 1443546);
    }

    @Test
    public void testPathVariableResolution1() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException {
        ///Test path variable resolution
        Optional<ServiceRegistry.MethodContext> opt2 = registry.getMethodContext(RequestMethod.GET, new Uri("/test/user.name/messages/clear"));
        Assert.assertTrue(opt2.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt2.get().rootCtx);

        //(?!/test/)([.a-z0-9]+)(?=/messages/clear)
        //([A-Za-z0-9._@]+)$
        Response rsp = registry.invoke(RequestMethod.GET, new Uri("/test/user.name/messages/clear"), null);
        Assert.assertTrue(rsp.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp.getBody() == null);
    }

    @Test
    public void testPathVariableResolution2() throws MethodNotFoundException, ExecutionException, MissingParameterException, UnsupportedEncodingException {
        Optional<ServiceRegistry.MethodContext> optGbdi = registry.getMethodContext(RequestMethod.GET, new Uri("/1212212/devices/2323434"));
        Assert.assertTrue(optGbdi.isPresent());
        //(?!/)([.a-z0-9]+)(?=/devices/)
        Response<Device> rsp = registry.invoke(RequestMethod.GET, new Uri("/1212212/devices/2323434"), null);
        Assert.assertTrue(rsp.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp.getBody().getId() == 2323434l);

        Optional<ServiceRegistry.MethodContext> optGbdi1 = registry.getMethodContext(RequestMethod.GET, new Uri("/1212212/devices/switches/1998"));
        Assert.assertTrue(optGbdi1.isPresent());
        Response<Device> rsp1 = registry.invoke(RequestMethod.GET, new Uri("/1212212/devices/switches/1998"), null);
        Assert.assertTrue(rsp1.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp1.getBody().getId() == 1998);
    }

    @Test
    public void testPathVariableResolution3() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException {
        Optional<ServiceRegistry.MethodContext> optGbdi2 = registry.getMethodContext(RequestMethod.GET, new Uri("/1212212/devices/getById/1122321"));
        Assert.assertTrue(optGbdi2.isPresent());
        Response<Device> rsp = registry.invoke(RequestMethod.GET, new Uri("/1212212/devices/getById/1122321"), null);
        Assert.assertTrue(rsp.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp.getBody().getId() == 1122321l);
        ///----------------------------
    }

    @Test(expected = MethodNotFoundException.class)
    public void testNotImplemented() throws MethodNotFoundException, ExecutionException, MissingParameterException, UnsupportedEncodingException {

        Optional<ServiceRegistry.MethodContext> opt3 = registry.getMethodContext(RequestMethod.GET, new Uri("/test/user/name/messages/clear"));
        Assert.assertTrue(!opt3.isPresent());

        Response rspX = registry.invoke(RequestMethod.GET, new Uri("/test/user/name/messages/clear"), null);
        Assert.assertTrue(rspX.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rspX.getBody() == null);
    }

    @Test
    public void testOverloadedMethods() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException {
        ///Test overloaded methods
        Optional<ServiceRegistry.MethodContext> elOpt1 = registry.getMethodContext(RequestMethod.GET, new Uri("/222/devices/owner"));
        Assert.assertTrue(elOpt1.isPresent());

        Response<Device> rsp1 = registry.invoke(RequestMethod.GET, new Uri("/222/devices/owner"), null);
        Assert.assertTrue(rsp1.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp1.getBody().getLogicalName().equalsIgnoreCase("some device"));

        Optional<ServiceRegistry.MethodContext> elOpt2 = registry.getMethodContext(RequestMethod.GET, new Uri("/222/devices/owner?arg0=222"));
        Assert.assertTrue(elOpt2.isPresent());

        Response<Device> rsp2 = registry.invoke(RequestMethod.GET, new Uri("/222/devices/owner?arg0=222"), null);
        Assert.assertTrue(rsp2.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp2.getBody().getLogicalName().equalsIgnoreCase("some device"));
        Assert.assertTrue(rsp2.getBody().getId() == 222);
        Assert.assertTrue(rsp2.getBody().getTreshold() == null);

        Optional<ServiceRegistry.MethodContext> elOpt3 = registry.getMethodContext(RequestMethod.GET, new Uri("/222/devices/owner?arg0=222&arg1=114"));
        Assert.assertTrue(elOpt3.isPresent());

        Response<Device> rsp3 = registry.invoke(RequestMethod.GET, new Uri("/222/devices/owner?arg0=222&arg1=114"), null);
        Assert.assertTrue(rsp3.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp3.getBody().getLogicalName().equalsIgnoreCase("some device"));
        Assert.assertTrue(rsp3.getBody().getId() == 222);
        Assert.assertTrue(rsp3.getBody().getTreshold() == 114);

        Optional<ServiceRegistry.MethodContext> elOpt4 = registry.getMethodContext(RequestMethod.GET, new Uri("/222/devices/owner/old"));
        Assert.assertTrue(elOpt4.isPresent());

        Response<Device> rsp4 = registry.invoke(RequestMethod.GET, new Uri("/222/devices/owner/old"), null);
        Assert.assertTrue(rsp4.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp4.getBody().getLogicalName().equalsIgnoreCase("some device"));
        ///----------------------------
    }

    @Test
    public void testReqMethodWithReqMapping() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException {
        ///Test Request Response with request mapping
        Optional<ServiceRegistry.MethodContext> opt8 = registry.getMethodContext(RequestMethod.GET, new Uri("/test/user@name/messages/sum"));
        Assert.assertTrue(opt8.isPresent());
        //(?!/test/)([.@a-z0-9]+)(?=/messages/sum)
        //(?!/test/)([A-Za-z0-9._@]+)(?=/messages/sum)
        Request<Tuple<Integer, Integer>> req = new Request<>(new Request.RequestHeader(HttpMethod.GET), new Tuple<>(10, 10));
        Response<Integer> rsp1 = registry.invoke(RequestMethod.GET, new Uri("/test/user@name/messages/sum"), req);
        Assert.assertTrue(rsp1.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp1.getBody() == 20);

        Optional<ServiceRegistry.MethodContext> opt9 = registry.getMethodContext(RequestMethod.GET, new Uri("/some_user/devices/listAll"));
        Assert.assertTrue(opt9.isPresent());

        Response<ArrayList<Device>> rsp2 = registry.invoke(RequestMethod.GET, new Uri("/some_user/devices/listAll"), null);
        Assert.assertTrue(rsp2.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(0 == rsp2.getBody().size());
    }

    @Test
    public void testInterfaceMarkedServices() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException {
        ///Test interface marked services
        Optional<ServiceRegistry.MethodContext> opt4 = registry.getMethodContext(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/DummyMarkedEndpoint/"));
        Assert.assertTrue(!opt4.isPresent());

        Optional<ServiceRegistry.MethodContext> opt5 = registry.getMethodContext(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/DummyMarkedEndpoint/echo"));
        Assert.assertTrue(opt5.isPresent());

        Response<String> rsp1 = registry.invoke(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/DummyMarkedEndpoint/echo"), ParameterWrapper.create().add("arg0", String.class, "\"something in the rain\""));
        Assert.assertTrue(rsp1.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp1.getBody().equalsIgnoreCase("Greetings earthlings: " + "\"something in the rain\""));

        Response<String> rsp2 = registry.invoke(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/DummyMarkedEndpoint/echo"), ParameterWrapper.create().add("arg0", String.class, "something in the rain"));
        Assert.assertTrue(rsp2.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp2.getBody().equalsIgnoreCase("Greetings earthlings: " + "something in the rain"));
        ///----------------------------
    }

    @Test
    public void testRpcServices1() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException {
        //Test RPC interfaces
        Optional<ServiceRegistry.MethodContext> opt6 = registry.getMethodContext(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/RpcEndpoint/ping"));
        Assert.assertTrue(!opt6.isPresent());

        Optional<ServiceRegistry.MethodContext> opt7 = registry.getMethodContext(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/ping"));
        Assert.assertTrue(opt7.isPresent());

        Response rsp = registry.invoke(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/ping"), null);
        Assert.assertTrue(rsp.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp.getBody() == null);
    }

    @Test
    public void testRpcServices2() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException {
        Response<String> rsp1 = registry.invoke(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/echo"), ParameterWrapper.create().add("arg0", String.class, "Hello people"));
        Assert.assertTrue(rsp1.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp1.getBody().equalsIgnoreCase("Hello people"));
    }

    @Test
    public void testRpcServices3() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException {
        Response<Filter> rsp2 = registry.invoke(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/getInfo"), null);
        Assert.assertTrue(rsp2.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp2.getBody().getQuery().equalsIgnoreCase("some query"));
        Assert.assertTrue(rsp2.getBody().getPage() == 10);
        Assert.assertTrue(rsp2.getBody().getPageSize() == 100);
    }

    @Test
    public void testRpcServices4() throws UnsupportedEncodingException, MethodNotFoundException, ExecutionException, MissingParameterException {
        Response<Filter> rsp3 = registry.invoke(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/filter"), ParameterWrapper.create().add("arg0", Filter.class, new Filter("other query", Integer.MAX_VALUE, Integer.MAX_VALUE)));
        Assert.assertTrue(rsp3.getHeader().getResponseCode() == HttpResponseStatus.OK);
        Assert.assertTrue(rsp3.getBody().getPage() == 100);
        Assert.assertTrue(rsp3.getBody().getPageSize() == 200);
    }

    @Test(expected = MethodNotFoundException.class)
    public void testNegative() throws MethodNotFoundException, ExecutionException, MissingParameterException, UnsupportedEncodingException {
        Response<Filter> rsp1 = registry.invoke(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/getInfo?arg0=some param"), null);
    }
}
