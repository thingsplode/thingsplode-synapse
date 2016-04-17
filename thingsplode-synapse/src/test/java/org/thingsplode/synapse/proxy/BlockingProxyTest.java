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

import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.thingsplode.synapse.AbstractTest;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;

/**
 *
 * @author Csaba Tamas
 */
public class BlockingProxyTest extends AbstractTest {

    public AbstractTest at = null;
    public static Dispatcher defaultDispatcher;

    public BlockingProxyTest() {
    }

    @BeforeClass
    public static void setUpClass() throws SSLException, InterruptedException, URISyntaxException {
        

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

    void display(Object r) {
        System.out.println("->" + r);
    }

    @Test
    public void baseBlockingRequestTimeoutTest() throws InterruptedException, UnsupportedEncodingException, ExecutionException {
        Assert.assertNotNull("the dispacther must not be null", defaultDispatcher);
        DispatcherFuture<Request, Response> f = defaultDispatcher.dispatch(Request.create("/com/acme/synapse/testdata/services/RpcEndpointImpl/ping", Request.RequestHeader.RequestMethod.GET), 100);
        int code = f.handle((rsp, ex) -> {
            if (rsp != null) {
                System.out.println("RESPONSE RECEIVED@Test Case => " + (rsp.getHeader() != null ? rsp.getHeader().getResponseCode() : "NULL RSP CODE") + " //Body: " + rsp.getBody());
                return 1;
            } else {
                System.out.println("\n\nERROR -> " + ex.getMessage() + "\n\n");
                Assert.assertTrue("Execution should time out", ex != null);
                ex.printStackTrace();
                return -1;
            }
        }).get();
        Assert.assertTrue("The return code must be -1 because the message must have timed our", code == -1);
    }

    @Test()
    public void baseBlockingRequestTest() throws InterruptedException, URISyntaxException, SSLException, UnsupportedEncodingException, ExecutionException {
        Assert.assertNotNull("the dispacther must not be null", defaultDispatcher);
        //Thread.sleep(2000);
        DispatcherFuture<Request, Response> f = defaultDispatcher.dispatch(Request.create("/com/acme/synapse/testdata/services/RpcEndpointImpl/ping", Request.RequestHeader.RequestMethod.GET), 6000);
        int code = f.handle((rsp, ex) -> {
            if (rsp != null) {
                System.out.println("RESPONSE RECEIVED@Test Case => " + (rsp.getHeader() != null ? rsp.getHeader().getResponseCode() : "NULL RSP CODE") + " //Body: " + rsp.getBody());
                Assert.assertTrue("Execution should be scuccessfull", rsp.getHeader().getResponseCode().equals(HttpResponseStatus.OK));
                return 1;
            } else {
                System.out.println("\n\nERROR -> " + ex.getMessage() + "\n\n");
                return -1;
            }
        }).get();
        Assert.assertTrue("The return code must be 1 so no error is returned", code == 1);

        //response introspector line 50
        //server-timeout handlers on the client
//        while (true) {
//            Thread.sleep(50000L);
//        }
//        EndpointProxy proxy = EndpointProxy.init().endpoints().defaultPolicy().start();
//        TestEndpoint testEp = proxy.createStub("test_service", TestEndpoint.class);
//        testEp.ping();
//        String msg = "hello world!";
//        Assert.assertEquals(String.format("Expected message: %s", msg), msg, testEp.echo(msg));
//        Filter f = new Filter("select * from something", 1, 100);
//        Assert.assertTrue("The result should be a filter class", testEp.filter(f) instanceof Filter);
//        Assert.assertEquals("The page size should be 200", new Integer(200), ((Filter) testEp.filter(f)).getPageSize());
    }

    //todo: broadcast test: //defaultDispatcher.broadcast(Request.create("/com/acme/synapse/testdata/services/RpcEndpointImpl/ping", Request.RequestHeader.RequestMethod.GET));
}
