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

import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.thingsplode.synapse.TestEventProcessor;
import org.thingsplode.synapse.core.domain.ParameterWrapper;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.RequestMethod;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.exceptions.RequestTimeoutException;
import org.thingsplode.synapse.core.domain.Event;

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
        DispatchedFuture<Request, Response> f = dispatcher.dispatch(Request.create(serviceMethod, RequestMethod.GET, pw), 100);
        int code = f.handle((rsp, ex) -> {
            if (rsp != null) {
                System.out.println("RESPONSE RECEIVED@Test Case => " + (rsp.getHeader() != null ? rsp.getHeader().getResponseCode() : "NULL RSP CODE") + " //Body: " + rsp.getBody());
                return 1;
            } else {
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
        pw.add("arg0", Long.TYPE, 1000);
        pw.add("arg1", String.class, uuid);
        DispatchedFuture<Request, Response> f = dispatcher.dispatch(Request.create(serviceMethod, RequestMethod.GET, pw), 3000);
        int code = f.handle((rsp, ex) -> {
            if (rsp != null) {
                System.out.println("RESPONSE RECEIVED@Test Case => " + (rsp.getHeader() != null ? rsp.getHeader().getResponseCode() : "NULL RSP CODE") + " //Body: " + rsp.getBody());
                Assert.assertTrue("Execution should be scuccessfull", rsp.getHeader().getResponseCode().equals(HttpResponseStatus.OK));
                Assert.assertTrue("The response type should be string, but is: " + (rsp.getBody() != null ? rsp.getBody().getClass() : "null"), rsp.getBody() instanceof String);
                Assert.assertTrue("the response uuid should match the request uuid", ((String) rsp.getBody()).equals(uuid));
                return 1;
            } else {
                System.out.println("\n\nTEST ERROR -> " + ex.getMessage() + "| returning -1.\n\n");
                return -1;
            }
        }).get();
        Assert.assertTrue("The return code must be 1 so no error is returned", code == 1);
        System.out.println("--------EOF " + header + " --------\n\n");
    }

    public static void sequentialTest(String header, Dispatcher dispatcher) throws InterruptedException, UnsupportedEncodingException, ExecutionException {
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
        System.out.println("-------- EOF " + header + " --------\n\n");
    }

    public static void burstTest(String header, Dispatcher dispatcher) throws UnsupportedEncodingException, InterruptedException {
        int msgCnt = 1000;
        ArrayBlockingQueue requestQueue = new ArrayBlockingQueue(msgCnt);
        System.out.println("\n\n*** " + header + " > Thread: " + Thread.currentThread().getName());
        Assert.assertNotNull("the dispacther must not be null", dispatcher);
        String serviceMethod = "/com/acme/synapse/testdata/services/RpcEndpointImpl/echoWithTimeout";
        for (int i = 0; i < msgCnt; i++) {
            System.out.println("\n\n============== MSG COUNTER [" + i + "] ==============");
            String uuid = UUID.randomUUID().toString();
            ParameterWrapper pw = new ParameterWrapper();
            pw.add("arg0", Long.TYPE, 0);
            pw.add("arg1", String.class, uuid);
            DispatchedFuture<Request, Response> future = dispatcher.dispatch(Request.create(serviceMethod, RequestMethod.GET, pw), 10000);
            requestQueue.add(future);
            future.handle((rsp, ex) -> {
                if (rsp != null) {
                    System.out.println("RESPONSE RECEIVED@Test Case => " + (rsp.getHeader() != null ? rsp.getHeader().getResponseCode() : "NULL RSP CODE") + " //Body: " + rsp.getBody());
                    Assert.assertTrue("Execution should be scuccessfull", rsp.getHeader().getResponseCode().equals(HttpResponseStatus.OK));
                    Assert.assertTrue("The response type should be string, but is: " + (rsp.getBody() != null ? rsp.getBody().getClass() : "null"), rsp.getBody() instanceof String);
                    Assert.assertTrue("the response uuid should match the request uuid", ((String) rsp.getBody()).equals(uuid));
                    requestQueue.remove(future);
                } else {
                    System.out.println("\n\nTEST Exception -> " + ex.getMessage() + "| returning -1\n\n");
                    Assert.fail("None of the request should time out.");
                }
                Assert.assertTrue(rsp != null);
                return "";
            });
        }
        while (!requestQueue.isEmpty()) {
        }
    }

    public static void testEvent(String header, Dispatcher dispatcher) throws UnsupportedEncodingException, InterruptedException {
        //System.out.println("\n\n*** " + header + " > Thread: " + Thread.currentThread().getName());
        //dispatcher.broadcast(Event.create("/default/")).await();
        //TestEventProcessor.eventQueue.take();
    }
}
