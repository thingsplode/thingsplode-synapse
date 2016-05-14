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

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.thingsplode.synapse.AbstractTest;
import org.thingsplode.synapse.TestEventProcessor;

/**
 *
 * @author Csaba Tamas
 */
public class BlockingWsProxyTest extends AbstractTest {

    @ClassRule
    public static ExternalResource clientProxy = new ExternalResource() {

        private EndpointProxy epx;

        @Override
        protected void before() {
            try {
                epx = EndpointProxy.create("ws://localhost:8080/", Dispatcher.DispatcherPattern.BLOCKING_REQUEST).
                        enableIntrospection().
                        setRetryConnection(true).initialize();
                dispatcher = epx.acquireDispatcher();
            } catch (URISyntaxException | SSLException | InterruptedException th) {
                System.out.println("\n\n\nERROR while setting up: " + BlockingProxyTest.class.getSimpleName() + ". Dumping stack trace: ");
                th.printStackTrace();
            }
        }

        @Override
        protected void after() {
            epx.stop();
        }

        public Dispatcher getDispatcher() {
            return dispatcher;
        }

        public EndpointProxy getEpx() {
            return epx;
        }
    };

    public BlockingWsProxyTest() {
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
        TestTemplates.eventTestWithWrongAddress("BLOCKING EVENT WITH WRONG TARGET", dispatcher);

    }

    @Test
    public void eventTest() throws UnsupportedEncodingException, InterruptedException, ExecutionException {
        TestTemplates.eventTest("BLOCKING EVENT", dispatcher);
    }

    @Test
    public void burstEventTest() throws UnsupportedEncodingException, InterruptedException {
        TestTemplates.burstEventTest("BLOCKING BURST EVENT TEST", dispatcher);
    }

    //@Test
    public void blockingTest() throws InterruptedException {
        TestEventProcessor.eventQueue.poll(10, TimeUnit.MINUTES);
    }

    //todo: dispatching on multiple threads....
    //todo: error 400 Body type not supported -> message is missing
    //todo: test with service published in the root context /
    //todo: test swagger defintions
    //todo: swagger reads from the class path and list services which might not be published
    //todo: testing already bound exception
    //todo: broadcast test: //defaultDispatcher.broadcast(Request.create("/com/acme/synapse/testdata/services/RpcEndpointImpl/ping", Request.RequestHeader.RequestMethod.GET));
}
