/*
 * Copyright 2016 Csaba Tamas.
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

import org.thingsplode.synapse.DispatchedFuture;
import com.acme.synapse.testdata.services.core.Address;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.thingsplode.synapse.TestEventProcessor;
import org.thingsplode.synapse.core.ParameterWrapper;
import org.thingsplode.synapse.core.Request;
import org.thingsplode.synapse.core.RequestMethod;
import org.thingsplode.synapse.core.Response;
import org.thingsplode.synapse.core.exceptions.RequestTimeoutException;
import org.thingsplode.synapse.core.Event;

/**
 *
 * @author Csaba Tamas
 */
public class TestTemplates {

    public static void baseRequestTimeoutTest(String header, Dispatcher dispatcher) throws InterruptedException, UnsupportedEncodingException, ExecutionException {
        System.out.println("\n\n*** " + header + " > Thread: " + Thread.currentThread().getName());
        Assert.assertNotNull("the dispacther must not be null", dispatcher);
        String serviceMethod = "/com/acme/synapse/testdata/services/RpcEndpointImpl/echoWithTimeout";
        String uuid = UUID.randomUUID().toString();
        ParameterWrapper pw = new ParameterWrapper();
        pw.add("arg0", Long.TYPE, 2000);
        pw.add("arg1", String.class, uuid);
        DispatchedFuture<Request, Response> f = dispatcher.dispatch(Request.create(serviceMethod, RequestMethod.GET, pw), 500);
        int code = f.handle((rsp, ex) -> {
            if (rsp != null) {
                System.out.println("RESPONSE RECEIVED@Test Case => " + (rsp.getHeader() != null ? rsp.getHeader().getResponseCode() : "NULL RSP CODE") + " //Body: " + rsp.getBody());
                Assert.assertTrue("the request UUID should be equal to the response body", uuid.equalsIgnoreCase((String) rsp.getBody()));
                System.out.println("WATCHME -> rsp not null");
                System.out.println("uuid \t\t [" + uuid + "]");
                System.out.println("rsp body \t\t [" + rsp.getBody() + "]");
                return 1;
            } else {
                System.out.println("WATCHME -> rsp null and ex instanceof " + ex.getClass().getSimpleName() + "with message " + ex.getMessage());
                //ex.printStackTrace();
                //Thread.dumpStack();
                Assert.assertTrue("Execution should time out", ex instanceof RequestTimeoutException);
                return -1;
            }
        }).get();
        Assert.assertTrue("The return code must be -1 because the message must have timed out", code == -1);
        System.out.println("--------EOF " + header + " --------\n\n");
    }

    public static void normalRequestTest(String header, Dispatcher dispatcher) throws InterruptedException, UnsupportedEncodingException, ExecutionException {
        System.out.println("\n\n*** " + header + " > Thread: " + Thread.currentThread().getName());
        Assert.assertNotNull("the dispacther must not be null", dispatcher);
        String serviceMethod = "/com/acme/synapse/testdata/services/RpcEndpointImpl/echoWithTimeout";
        String uuid = UUID.randomUUID().toString();
        ParameterWrapper pw = new ParameterWrapper();
        pw.add("arg0", Long.TYPE, 999);
        pw.add("arg1", String.class, uuid);
        DispatchedFuture<Request, Response> f = dispatcher.dispatch(Request.create(serviceMethod, RequestMethod.GET, pw), 10000);
        int code = f.handle((rsp, ex) -> {
            if (rsp != null) {
                System.out.println("RESPONSE RECEIVED@Test Case => " + (rsp.getHeader() != null ? rsp.getHeader().getResponseCode() : "NULL RSP CODE") + " //Body: " + rsp.getBody());
                Assert.assertTrue("Execution should be scuccessfull", rsp.getHeader().getResponseCode().equals(HttpResponseStatus.OK));
                Assert.assertTrue("The response type should be string, but is: " + (rsp.getBody() != null ? rsp.getBody().getClass() : "null"), rsp.getBody() instanceof String);
                Assert.assertTrue("the response uuid should match the request uuid", ((String) rsp.getBody()).equals(uuid));
                return 1;
            } else {
                System.out.println("\n\nException ----> " + ex != null ? (ex.getClass().getSimpleName() + " || " + ex.getMessage()) : "{ex null}");
                System.out.println("\n\nTEST ERROR -> " + ex.getMessage() + "| returning -1.\n\n");
                ex.printStackTrace();
                return -1;
            }
        }).get();
        Assert.assertTrue("The return code must be 1 so no error is returned", code == 1);
        System.out.println("--------EOF " + header + " --------\n\n");
    }

    public static void sequentialTest(String header, Dispatcher dispatcher) throws InterruptedException, UnsupportedEncodingException, ExecutionException {
        List<Throwable> errorQueue = new ArrayList<>();
        int msgCnt = 10;
        System.out.println("\n\n*** " + header + " > Thread: " + Thread.currentThread().getName());
        Assert.assertNotNull("the dispacther must not be null", dispatcher);
        String serviceMethod = "/com/acme/synapse/testdata/services/RpcEndpointImpl/echoWithTimeout";
        boolean timeout = false;
        for (int i = 0; i < msgCnt; i++) {
            System.out.println("\n\n============== MSG COUNTER [" + i + "] ==============");
            String uuid = UUID.randomUUID().toString();
            ParameterWrapper pw = new ParameterWrapper();
            pw.add("arg0", Long.TYPE, timeout ? 2000 : 500);
            pw.add("arg1", String.class, uuid);
            DispatchedFuture<Request, Response> f = dispatcher.dispatch(Request.create(serviceMethod, RequestMethod.GET, pw), 1000);
            int code = f.handle((rsp, ex) -> {
                if (rsp != null) {
                    System.out.println("RESPONSE RECEIVED@Test Case => " + (rsp.getHeader() != null ? rsp.getHeader().getResponseCode() : "NULL RSP CODE") + " //Body: " + rsp.getBody());
                    Assert.assertTrue("Execution should be scuccessfull", rsp.getHeader().getResponseCode().equals(HttpResponseStatus.OK));
                    Assert.assertTrue("The response type should be string, but is: " + (rsp.getBody() != null ? rsp.getBody().getClass() : "null"), rsp.getBody() instanceof String);
                    Assert.assertTrue("the response uuid should match the request uuid", ((String) rsp.getBody()).equals(uuid));
                    return 1;
                } else {
                    if (ex != null && !(ex instanceof RequestTimeoutException)) {
                        System.out.println("WATCH HERE -- THIS IS NOT TIMEOUT --> " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                        //ex.printStackTrace();
                        errorQueue.add(ex);
                    }
                    System.out.println("\n\nTEST Exception -> " + ex.getMessage() + "| returning -1\n\n");
                    return -1;
                }
            }).get();

            if (!timeout) {
                Assert.assertTrue("The return code must be 1 because error code should have not been returned.", code == 1);
            } else {
                Assert.assertTrue("The return code must be -1, because the message must have timed out.", code == -1);
            }
            timeout = !timeout;
        }
        if (errorQueue.size() > 0) {
            for (Throwable th : errorQueue) {
                System.out.println("TEST ERROR > " + th.getClass().getSimpleName() + ": " + th.getMessage());
            }
        }
        Assert.assertTrue("The error queue must be emtpy", errorQueue.isEmpty());
        System.out.println("-------- EOF " + header + " --------\n\n");
    }

    public static void burstTest(String header, Dispatcher dispatcher) throws UnsupportedEncodingException, InterruptedException {
        int msgCnt = 1000;
        ArrayBlockingQueue requestQueue = new ArrayBlockingQueue(msgCnt);
        ArrayBlockingQueue failQueue = new ArrayBlockingQueue(msgCnt);
        System.out.println("\n\n*** " + header + " > Thread: " + Thread.currentThread().getName());
        Assert.assertNotNull("the dispacther must not be null", dispatcher);
        String serviceMethod = "/com/acme/synapse/testdata/services/RpcEndpointImpl/echoWithTimeout";
        Counter cnt = new Counter();
        for (int i = 0; i < msgCnt; i++) {
            System.out.println("\n\n============== MSG COUNTER [" + i + "] ==============");
            String uuid = UUID.randomUUID().toString();
            ParameterWrapper pw = new ParameterWrapper();
            pw.add("arg0", Long.TYPE, 0);
            pw.add("arg1", String.class, uuid);
            DispatchedFuture<Request, Response> future = dispatcher.dispatch(Request.create(serviceMethod, RequestMethod.GET, pw), 10000);
            requestQueue.add(future);
            future.handle((rsp, ex) -> {
                requestQueue.remove(future);
                if (rsp != null) {
                    System.out.println("RESPONSE RECEIVED@Test Case => " + (rsp.getHeader() != null ? rsp.getHeader().getResponseCode() : "NULL RSP CODE") + " //Body: " + rsp.getBody());
                    Assert.assertTrue("Execution should be scuccessfull", rsp.getHeader().getResponseCode().equals(HttpResponseStatus.OK));
                    Assert.assertTrue("The response type should be string, but is: " + (rsp.getBody() != null ? rsp.getBody().getClass() : "null"), rsp.getBody() instanceof String);
                    Assert.assertTrue("the response uuid should match the request uuid", ((String) rsp.getBody()).equals(uuid));
                } else {
                    failQueue.offer(true);
                    if (ex != null && !(ex instanceof RequestTimeoutException)) {
                        System.out.println("WATCH HERE --> " + ex.getMessage());
                        ex.printStackTrace();
                    }
                    System.out.println("\n\nTEST Exception -> " + ex.getMessage() + "| returning -1\n\n");
                    Assert.fail("None of the request should time out.");
                }
                Assert.assertTrue(rsp != null);
                return "";
            });
        }
        ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
        es.scheduleAtFixedRate(() -> {
            System.out.println("\n\n\n\n\nRequest queue size at burst test: " + requestQueue.size());
            //System.out.println("[Content of the request queue: ]" + requestQueue);
            System.out.println("Incremented counter: " + cnt.counter.incrementAndGet());
        }, 0, 60, TimeUnit.SECONDS);
        while (!requestQueue.isEmpty() && cnt.counter.get() < 3) {
        }
        if (failQueue.size() > 0) {
            Assert.fail("There were multiple (" + failQueue.size() + ") failures;");
        }
    }

    public static void eventTestWithWrongAddress(String header, Dispatcher dispatcher) throws UnsupportedEncodingException, InterruptedException, ExecutionException {
        System.out.println("\n\n*** " + header + " > Thread: " + Thread.currentThread().getName());
        boolean success = dispatcher.dispatch(Event.create("/default/"), 500).handle((rsp, ex) -> {
            if (rsp != null) {
                System.out.println("\n\nResponse@Test received with status: [" + rsp.getHeader().getResponseCode().toString() + "]");
                Assert.assertTrue("it must not accept the message", !rsp.getHeader().getResponseCode().equals(HttpResponseStatus.ACCEPTED));
                return !rsp.getHeader().getResponseCode().equals(HttpResponseStatus.ACCEPTED);
            } else {
                ex.printStackTrace();
                return false;
            }
        }).get();
        Assert.assertTrue("Execution should be successfull", success);
        //attention: without synchronous get() on the CompletableFuture (DispatchedFuture) the assertions are not working
    }

    public static void eventTest(String header, Dispatcher dispatcher) throws UnsupportedEncodingException, InterruptedException, ExecutionException {
        System.out.println("\n\n*** " + header + " > Thread: " + Thread.currentThread().getName());
        Event evt = Event.create("/default/consume");
        evt.setBody(new Address("some street", "soem country", 53600));
        boolean success = dispatcher.dispatch(evt, 500).handle((rsp, ex) -> {
            if (rsp != null) {
                System.out.println("\n\nResponse@Test received with status: [" + rsp.getHeader().getResponseCode().toString() + "]");
                return rsp.getHeader().getResponseCode().equals(HttpResponseStatus.ACCEPTED);
            } else {
                return false;
            }
        }).get();
        Assert.assertTrue("the message must succeed.", success);
        Event<Serializable> event = TestEventProcessor.eventQueue.poll(10, TimeUnit.SECONDS);
        Assert.assertTrue("Event must not be null", event != null);
    }

    public static void burstEventTest(String header, Dispatcher dispatcher) throws UnsupportedEncodingException, InterruptedException {
        System.out.println("\n\n*** " + header + " > Thread: " + Thread.currentThread().getName());
        int msgCnt = 999;
        ArrayBlockingQueue<DispatchedFuture> requestQueue = new ArrayBlockingQueue(msgCnt, true);
        ArrayBlockingQueue failQueue = new ArrayBlockingQueue(msgCnt);
        Counter cnt = new Counter();
        for (int i = 0; i < msgCnt; i++) {
            Event evt = Event.create("/default/consume");
            evt.setBody(new Address("some street", "soem country", msgCnt));
            DispatchedFuture<Request, Response> df = dispatcher.dispatch(evt, 5000);
            requestQueue.add(df);
            System.out.println("REQUEST COUNTER AFTER ADD: " + requestQueue.size());
            df.handle((rsp, ex) -> {
                try {
                    requestQueue.take();
                    System.out.println("REQUEST COUNTER AFTER TAKE: " + requestQueue.size());
                } catch (InterruptedException ex1) {
                    System.out.println("TEST CASE IMPLEMENTATION ERROR ===> " + ex.getMessage());
                }
                if (rsp != null) {
                    System.out.println("\n\nResponse@Test received with status: [" + rsp.getHeader().getResponseCode().toString() + "]");
                    boolean status = rsp.getHeader().getResponseCode().equals(HttpResponseStatus.ACCEPTED);
                    Assert.assertTrue("All events should be processed.", status);
                    return status;
                } else if (ex != null) {
                    System.out.println("--> TEST FAILURE: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    //ex.printStackTrace();
                    failQueue.add(true);
                    return false;
                } else {
                    System.out.println("\n\n --> TEST FAILURE: response is null & exception is null.");
                    failQueue.add(true);
                    return false;
                }
            });
        }

        ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
        es.scheduleAtFixedRate(() -> {
            System.out.println("\n\n\n\n\nRequest queue size at burst event test: " + requestQueue.size());
            System.out.println("[Content of the request queue: ]" + requestQueue);
            System.out.println("TestEventProcessor.eventQueue size: " + TestEventProcessor.eventQueue.size());
            System.out.println("Incremented counter: " + cnt.counter.incrementAndGet());
        }, 0, 60, TimeUnit.SECONDS);

        while (!TestEventProcessor.eventQueue.isEmpty()) {
            TestEventProcessor.eventQueue.poll();
        }
        while (!requestQueue.isEmpty() && cnt.counter.get() < 3) {
        }
        if (failQueue.size() > 0) {
            Assert.fail("There were multiple (" + failQueue.size() + ") failures;");
        }
    }

    static class Counter {

        AtomicInteger counter = new AtomicInteger(0);
    }
}
