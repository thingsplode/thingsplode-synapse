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
package org.thingsplode.synapse.endpoint.srvreg;

import com.acme.synapse.testdata.services.CrudTestEndpointService;
import com.acme.synapse.testdata.services.DummyMarkedEndpoint;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.acme.synapse.testdata.services.RpcEndpointImpl;
import com.acme.synapse.testdata.services.TestSecondEndpointService;
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import org.junit.Assert;
import org.thingsplode.synapse.core.Uri;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class InternalServiceRegistryTest {

    private InternalServiceRegistry registry = new InternalServiceRegistry();

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
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testRegistration() throws UnsupportedEncodingException {
        registry.register(new RpcEndpointImpl());
        registry.register(new DummyMarkedEndpoint());
        registry.register(new TestSecondEndpointService());
        registry.register(new CrudTestEndpointService());

        Optional<InternalServiceRegistry.MethodContext> opt = registry.matchUri(new Uri("/test/user.name/messages/calculate"));
        Assert.assertTrue(opt.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt.get().rootCtx);

        Optional<InternalServiceRegistry.MethodContext> opt1 = registry.matchUri(new Uri("/test/user.name/messages/add"));
        Assert.assertTrue(opt1.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt1.get().rootCtx);

        Optional<InternalServiceRegistry.MethodContext> opt2 = registry.matchUri(new Uri("/test/user.name/messages/clear"));
        Assert.assertTrue(opt2.isPresent());
        Assert.assertEquals("/test/{user}/messages", opt2.get().rootCtx);

        Optional<InternalServiceRegistry.MethodContext> opt3 = registry.matchUri(new Uri("/test/user/name/messages/clear"));
        Assert.assertTrue(!opt3.isPresent());

        Optional<InternalServiceRegistry.MethodContext> opt4 = registry.matchUri(new Uri("/com/acme/synapse/testdata/services/DummyMarkedEndpoint/"));
        Assert.assertTrue(!opt4.isPresent());

        Optional<InternalServiceRegistry.MethodContext> opt5 = registry.matchUri(new Uri("/com/acme/synapse/testdata/services/DummyMarkedEndpoint/echo"));
        Assert.assertTrue(opt5.isPresent());

        Optional<InternalServiceRegistry.MethodContext> opt6 = registry.matchUri(new Uri("/com/acme/synapse/testdata/services/RpcEndpoint/ping"));
        Assert.assertTrue(!opt6.isPresent());

        Optional<InternalServiceRegistry.MethodContext> opt7 = registry.matchUri(new Uri("/com/acme/synapse/testdata/services/RpcEndpointImpl/ping"));
        Assert.assertTrue(opt7.isPresent());

        Optional<InternalServiceRegistry.MethodContext> opt8 = registry.matchUri(new Uri("/test/user@name/messages/sum"));
        Assert.assertTrue(opt8.isPresent());
        
        Optional<InternalServiceRegistry.MethodContext> opt9 = registry.matchUri(new Uri("/some_user/devices/listAll"));
        Assert.assertTrue(opt9.isPresent());
    }
}
