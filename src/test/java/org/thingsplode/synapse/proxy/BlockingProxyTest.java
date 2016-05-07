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

import com.acme.synapse.testdata.services.core.Address;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.thingsplode.synapse.AbstractBlockingClientTest;
import org.thingsplode.synapse.TestEventProcessor;
import org.thingsplode.synapse.core.domain.Event;

/**
 *
 * @author Csaba Tamas
 */
public class BlockingProxyTest extends AbstractBlockingClientTest {

    public BlockingProxyTest() {
        super();
    }

    @BeforeClass
    public static void setUpClass() throws SSLException, InterruptedException, URISyntaxException {

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

    //todo: timing out non-existent method
    @Test
    public void baseBlockingRequestTimeoutTest() throws InterruptedException, UnsupportedEncodingException, ExecutionException {
        TestTemplates.baseRequestTimeoutTest("BLOCKING REQUEST TIMEOUT TEST", dispatcher);
    }

    //issue: when the first request times out, the second request receives the answer for the first request
    //ping should have a serial number and answer that one
    //the timeout generator should close the connection // but what will happen with non-http protocols
    @Test()
    public void baseBlockingRequestTest() throws InterruptedException, URISyntaxException, SSLException, UnsupportedEncodingException, ExecutionException {
        TestTemplates.normalRequestTest("SEQUENTIAL BLOCKING REQUEST EXECUTION TEST", dispatcher);
    }

    @Test
    public void sequentialMessageTest() throws InterruptedException, UnsupportedEncodingException, ExecutionException {
        TestTemplates.sequentialTest("SEQUENTIAL BLOCKING REQUEST EXECUTION TEST", dispatcher);
    }

    @Test
    public void burstTest() throws UnsupportedEncodingException, InterruptedException {
        TestTemplates.burstTest("BURST BLOCKING REQUEST EXECUTION TEST", dispatcher);
    }

    @Test
    public void eventTestWithWrongAddress() throws UnsupportedEncodingException, InterruptedException, ExecutionException {
        //TestTemplates.testEvent("BLOCKING EVENT", dispatcher);
        System.out.println("\n\n*** " + "BLOCKING EVENT WITH WRONG TARGET" + " > Thread: " + Thread.currentThread().getName());
        boolean success = dispatcher.dispatch(Event.create("/default/"), 1000).handle((rsp, ex) -> {
            if (rsp != null) {
                System.out.println("\n\nResponse@Test received with status: [" + rsp.getHeader().getResponseCode().toString() + "]");
                return rsp.getHeader().getResponseCode() == HttpResponseStatus.ACCEPTED;

            } else {
                ex.printStackTrace();
                return false;
            }
        }).get();
        //Event<Serializable> event = TestEventProcessor.eventQueue.poll(10, TimeUnit.MINUTES);
    }

    @Test
    public void eventTest() throws UnsupportedEncodingException, InterruptedException, ExecutionException {
        System.out.println("\n\n*** " + "BLOCKING EVENT" + " > Thread: " + Thread.currentThread().getName());
        Event evt = Event.create("/default/consume");
        evt.setBody(new Address("some street", "soem country", 53600));
        boolean success = dispatcher.dispatch(evt, 30000).handle((rsp, ex) -> {
            if (rsp != null) {
                System.out.println("\n\nResponse@Test received with status: [" + rsp.getHeader().getResponseCode().toString() + "]");
                return rsp.getHeader().getResponseCode().equals(HttpResponseStatus.ACCEPTED);
            } else {
                ex.printStackTrace();
                return false;
            }
        }).get();
        Assert.assertTrue("the message must succeed.", success);
        Event<Serializable> event = TestEventProcessor.eventQueue.poll(10, TimeUnit.SECONDS);
        Assert.assertTrue("Event must not be null", event != null);
    }

    //todo: test with service published in the root context /
    //events should be processed with:  202 Accepted
    //swagger reads from the class path and list services which might not be published
    //todo: testing already bound exception
    //todo: broadcast test: //defaultDispatcher.broadcast(Request.create("/com/acme/synapse/testdata/services/RpcEndpointImpl/ping", Request.RequestHeader.RequestMethod.GET));
}
