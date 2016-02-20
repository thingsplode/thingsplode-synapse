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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.thingsplode.synapse.AbstractTest;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class ProxyTest extends AbstractTest {

    public AbstractTest at = null;

    public ProxyTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        if (at == null) {
            at = new AbstractTest() {
            };
        }
    }

    @After
    public void tearDown() {
        if (at != null && at.getEndpoint() != null) {
            at.getEndpoint().stop();
        }
    }

    @Test
    public void baseTest() throws InterruptedException {
        Thread.sleep(50000L);
//        EndpointProxy proxy = EndpointProxy.init().endpoints().defaultPolicy().start();
//        TestEndpoint testEp = proxy.createStub("test_service", TestEndpoint.class);
//        testEp.ping();
//        String msg = "hello world!";
//        Assert.assertEquals(String.format("Expected message: %s", msg), msg, testEp.echo(msg));
//        Filter f = new Filter("select * from something", 1, 100);
//        Assert.assertTrue("The result should be a filter class", testEp.filter(f) instanceof Filter);
//        Assert.assertEquals("The page size should be 200", new Integer(200), ((Filter) testEp.filter(f)).getPageSize());
    }
}
