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
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.AbstractTest;
import org.thingsplode.synapse.DispatchedFuture;
import org.thingsplode.synapse.TestCommandSink;
import org.thingsplode.synapse.TestNotificationSink;
import org.thingsplode.synapse.core.Command;
import org.thingsplode.synapse.core.CommandResult;
import org.thingsplode.synapse.core.PushNotification;

/**
 *
 * @author Csaba Tamas
 */
public class BlockingWsProxyTest extends AbstractTest {

    private final Logger logger = LoggerFactory.getLogger(BlockingWsProxyTest.class);

    @ClassRule
    public static ExternalResource clientProxy = new ExternalResource() {

        private EndpointProxy epx;

        @Override
        protected void before() {
            try {
                epx = EndpointProxy.create("ws://localhost:8080/", Dispatcher.DispatcherPattern.BLOCKING_REQUEST).
                        enableIntrospection()
                        .setRetryConnection(true)
                        .addCommandSink(new TestCommandSink())
                        .addDefaultNotificationSink(new TestNotificationSink())
                        .subscribeToNotificationTopic("someTopic", new TestNotificationSink())
                        .initialize();
                dispatcher = epx.acquireDispatcher();
            } catch (URISyntaxException | SSLException | InterruptedException th) {
                AbstractTest.logger.error("ERROR while setting up: " + BlockingProxyTest.class.getSimpleName() + ". Dumping stack trace.", th);
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

    //issue: when the first request times out, the second request receives the answer for the first request
    //ping should have a serial number and answer that one
    //the timeout generator should close the connection // but what will happen with non-http protocols
    @Test()
    public void baseBlockingRequestTest() throws InterruptedException, URISyntaxException, SSLException, UnsupportedEncodingException, ExecutionException {
        TestTemplates.normalRequestTest("SEQUENTIAL BLOCKING WS REQUEST EXECUTION TEST", dispatcher);
    }

    //todo: timing out non-existent method
    @Test
    public void baseBlockingRequestTimeoutTest() throws InterruptedException, UnsupportedEncodingException, ExecutionException {
        TestTemplates.baseRequestTimeoutTest("BLOCKING WS REQUEST TIMEOUT TEST", dispatcher);
    }

    @Test
    public void sequentialMessageTest() throws InterruptedException, UnsupportedEncodingException, ExecutionException {
        TestTemplates.sequentialTest("SEQUENTIAL BLOCKING WS REQUEST EXECUTION TEST", dispatcher);
    }

    @Test
    public void burstTest() throws UnsupportedEncodingException, InterruptedException {
        TestTemplates.burstTest("BURST BLOCKING WS REQUEST EXECUTION TEST", dispatcher);
    }

    @Test
    public void eventTestWithWrongAddress() throws UnsupportedEncodingException, InterruptedException, ExecutionException {
        TestTemplates.eventTestWithWrongAddress("BLOCKING WS EVENT WITH WRONG TARGET", dispatcher);

    }

    @Test
    public void eventTest() throws UnsupportedEncodingException, InterruptedException, ExecutionException {
        TestTemplates.eventTest("BLOCKING WS EVENT", dispatcher);
    }

    @Test
    public void burstEventTest() throws UnsupportedEncodingException, InterruptedException {
        TestTemplates.burstEventTest("BLOCKING WS BURST EVENT TEST", dispatcher);
    }

    @Test
    public void defaultPushNotificationTest() throws InterruptedException {
        ep.broadcast(new PushNotification(new PushNotification.NotificationHeader(3000)), 5000);
        PushNotification pn = TestNotificationSink.notificationQueue.poll(30, TimeUnit.SECONDS);
        Assert.assertNotNull(pn);
        Assert.assertTrue("It should come from the default notification topic.", pn.getHeader().getSourceTopic().equalsIgnoreCase(PushNotification.DEFAULT_NOTIFICATION_TOPIC));
    }

    @Test
    public void defaultXCommandTest() throws InterruptedException {
        logger.debug("DEFAUTL COMMAND TEST");
        final ArrayBlockingQueue<Boolean> endpointResultQueue = new ArrayBlockingQueue<>(1);
        List<DispatchedFuture> cmdInstances = ep.broadcast(new Command(new Command.CommandHeader(3000)), 5000);
        for (DispatchedFuture<Command, CommandResult> df : cmdInstances) {
            logger.trace("adding logic to command dispatch.");
            df.handle((res, ex) -> {
                if (res != null) {
                    try {

                        logger.debug("TEST --> COMMAND RESULT REECEIVED: " + res.toString());
                        endpointResultQueue.put(Boolean.TRUE);
                    } catch (InterruptedException ex1) {
                        logger.error("TEST CASE ERROR: couldn't place result in the result queue;");
                    }
                } else {
                    try {
                        logger.warn("TEST CASE: error while receiving command result.", ex);
                        endpointResultQueue.put(Boolean.FALSE);
                    } catch (InterruptedException ex1) {
                        logger.warn("TEST CASE ERROR: couldn't place result in the result queue;");
                    }
                }
                return null;
            });
        }
        Command cmdOnClient = TestCommandSink.commandQueue.poll(30, TimeUnit.SECONDS);
        Assert.assertNotNull("The command is not received after 30 sec.", cmdOnClient);
        Boolean res = endpointResultQueue.poll(30, TimeUnit.SECONDS);
        Assert.assertNotNull("The result must not be null", res);
        Assert.assertTrue("The result should be successfull.", res);

    }
    
    //todo: http blocking tests with keepalieve false -> creates issues with evicting the requests / those should be resent;
    //todo: command time to live and DST change
    //todo: write command processing with runtime exception test

    //todo: dispatching on multiple threads....
    //todo: error 400 Body type not supported -> message is missing
    //todo: test with service published in the root context /
    //todo: test swagger defintions
    //todo: swagger reads from the class path and list services which might not be published
    //todo: testing already bound exception
    //todo: broadcast test: //defaultDispatcher.broadcast(Request.create("/com/acme/synapse/testdata/services/RpcEndpointImpl/ping", Request.RequestHeader.RequestMethod.GET));
}
