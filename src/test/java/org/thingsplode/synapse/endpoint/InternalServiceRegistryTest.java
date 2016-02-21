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
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import org.junit.Assert;
import org.thingsplode.synapse.core.Uri;
import org.thingsplode.synapse.core.domain.RequestMethod;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class InternalServiceRegistryTest {

    private InternalServiceRegistry registry = new InternalServiceRegistry();
    private boolean inited = false;

    public InternalServiceRegistryTest() {
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
    public void testRequestMethodDispatching() throws UnsupportedEncodingException {
        ///test request method matching
        Optional<InternalServiceRegistry.MethodContext> optM = registry.getMethodContext(RequestMethod.GET, new Uri("/1221221/devices/add"));
        Assert.assertTrue(!optM.isPresent());

        Optional<InternalServiceRegistry.MethodContext> optM1 = registry.getMethodContext(RequestMethod.POST, new Uri("/1221221/devices/add"));
        Assert.assertTrue(optM1.isPresent());

        Optional<InternalServiceRegistry.MethodContext> optM2 = registry.getMethodContext(RequestMethod.PUT, new Uri("/1221221/devices/add"));
        Assert.assertTrue(optM2.isPresent());
        ///----------------------------
    }

    @Test
    public void testMultipleUrisSameMethod() throws UnsupportedEncodingException {
        ///Test multiple request uris to the same method and named parameters
        Optional<InternalServiceRegistry.MethodContext> opt = registry.getMethodContext(RequestMethod.GET, new Uri("/test/user.name/messages/calculate?a=2&b=3"));
        Assert.assertTrue(opt.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt.get().rootCtx);

        Optional<InternalServiceRegistry.MethodContext> opt1 = registry.getMethodContext(RequestMethod.GET, new Uri("/test/user.name/messages/add?a=1231434&b=212112"));
        Assert.assertTrue(opt1.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt1.get().rootCtx);
        ///----------------------------
    }

    @Test
    public void testPathVariableResolution() throws UnsupportedEncodingException {
        ///Test path variable resolution
        Optional<InternalServiceRegistry.MethodContext> opt2 = registry.getMethodContext(RequestMethod.GET, new Uri("/test/user.name/messages/clear"));
        Assert.assertTrue(opt2.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt2.get().rootCtx);

        Optional<InternalServiceRegistry.MethodContext> opt3 = registry.getMethodContext(RequestMethod.GET, new Uri("/test/user/name/messages/clear"));
        Assert.assertTrue(!opt3.isPresent());

        Optional<InternalServiceRegistry.MethodContext> optGbdi = registry.getMethodContext(RequestMethod.GET, new Uri("/1212212/devices/2323434"));
        Assert.assertTrue(optGbdi.isPresent());

        Optional<InternalServiceRegistry.MethodContext> optGbdi2 = registry.getMethodContext(RequestMethod.GET, new Uri("/1212212/devices/getById/1122321"));
        Assert.assertTrue(optGbdi2.isPresent());
        ///----------------------------
    }

    @Test
    public void testOverloadedMethods() throws UnsupportedEncodingException {
        ///Test overloaded methods
        Optional<InternalServiceRegistry.MethodContext> elOpt1 = registry.getMethodContext(RequestMethod.GET, new Uri("/222/devices/owner"));
        Assert.assertTrue(elOpt1.isPresent());

        Optional<InternalServiceRegistry.MethodContext> elOpt2 = registry.getMethodContext(RequestMethod.GET, new Uri("/222/devices/owner?arg0=222"));
        Assert.assertTrue(elOpt2.isPresent());

        Optional<InternalServiceRegistry.MethodContext> elOpt3 = registry.getMethodContext(RequestMethod.GET, new Uri("/222/devices/owner?arg0=222&arg1=114"));
        Assert.assertTrue(elOpt3.isPresent());

        Optional<InternalServiceRegistry.MethodContext> elOpt4 = registry.getMethodContext(RequestMethod.GET, new Uri("/222/devices/owner/old"));
        Assert.assertTrue(elOpt4.isPresent());
        ///----------------------------
    }

    @Test
    public void testReqMethodWithReqMapping() throws UnsupportedEncodingException {
        ///Test Request Response with request mapping
        Optional<InternalServiceRegistry.MethodContext> opt8 = registry.getMethodContext(RequestMethod.GET, new Uri("/test/user@name/messages/sum"));
        Assert.assertTrue(opt8.isPresent());

        Optional<InternalServiceRegistry.MethodContext> opt9 = registry.getMethodContext(RequestMethod.GET, new Uri("/some_user/devices/listAll"));
        Assert.assertTrue(opt9.isPresent());
    }

    @Test
    public void testInterfaceMarkedServices() throws UnsupportedEncodingException {
        ///Test interface marked services
        Optional<InternalServiceRegistry.MethodContext> opt4 = registry.getMethodContext(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/DummyMarkedEndpoint/"));
        Assert.assertTrue(!opt4.isPresent());

        Optional<InternalServiceRegistry.MethodContext> opt5 = registry.getMethodContext(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/DummyMarkedEndpoint/echo?arg0=\"something in the rain\""));
        Assert.assertTrue(opt5.isPresent());
        ///----------------------------
    }

    @Test
    public void testRpcServices() throws UnsupportedEncodingException {

        //Test RPC interfaces
        Optional<InternalServiceRegistry.MethodContext> opt6 = registry.getMethodContext(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/RpcEndpoint/ping"));
        Assert.assertTrue(!opt6.isPresent());

        Optional<InternalServiceRegistry.MethodContext> opt7 = registry.getMethodContext(RequestMethod.GET, new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/ping"));
        Assert.assertTrue(opt7.isPresent());
        ///----------------------------

    }
}
