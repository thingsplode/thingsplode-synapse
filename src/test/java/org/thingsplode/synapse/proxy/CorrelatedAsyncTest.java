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

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLException;
import org.junit.Test;
import org.thingsplode.synapse.AbstractCorrelatedClientTest;

/**
 *
 * @author Csaba Tamas
 */
public class CorrelatedAsyncTest extends AbstractCorrelatedClientTest {

    public CorrelatedAsyncTest() {
        super();
    }

    @Test
    public void baseCorrelatedRequestTimeoutTest() throws InterruptedException, UnsupportedEncodingException, ExecutionException {
        TestTemplates.baseRequestTimeoutTest("CORRELATED REQUEST TIMEOUT TEST", dispatcher);
    }

    //issue: when the first request times out, the second request receives the answer for the first request
    //ping should have a serial number and answer that one
    //the timeout generator should close the connection // but what will happen with non-http protocols
    @Test()
    public void baseCorrelatedRequestTest() throws InterruptedException, URISyntaxException, SSLException, UnsupportedEncodingException, ExecutionException {
        TestTemplates.normalRequestTest("BASE CORRELATED REQUEST EXECUTION TEST", dispatcher);
    }

    @Test
    public void sequentialCorrelatedMessageTest() throws InterruptedException, UnsupportedEncodingException, ExecutionException {
        TestTemplates.sequentialTest("SEQUENTIAL CORRELATED REQUEST EXECUTION TEST", dispatcher);
    }

    @Test
    public void burstTest() throws UnsupportedEncodingException, InterruptedException {
        TestTemplates.burstTest("BURST CORRELATED REQUEST EXECUTION TEST", dispatcher);
    }

    @Test
    public void eventTestWithWrongAddress() throws UnsupportedEncodingException, InterruptedException, ExecutionException {
        TestTemplates.eventTestWithWrongAddress("CORRELATED EVENT WITH WRONG TARGET", dispatcher);

    }

    @Test
    public void eventTest() throws UnsupportedEncodingException, InterruptedException, ExecutionException {
        TestTemplates.eventTest("CORRELATED EVENT NORMAL", dispatcher);
    }

    @Test
    public void burstEventTest() throws UnsupportedEncodingException, InterruptedException {
        TestTemplates.burstEventTest("CORRELATED BURST EVENT TEST", dispatcher);
    }

    @Test
    public void combinedErrorTest() throws InterruptedException, UnsupportedEncodingException, ExecutionException{
        TestTemplates.eventTestWithWrongAddress("CORRELATED EVENT WITH WRONG TARGET", dispatcher);
        // a redirect utan a csatornat lezarja a server, de meg elotte egy pillanattal ki lesz dispatcholva 
        // a kov. keres.
        TestTemplates.normalRequestTest("BASE CORRELATED REQUEST EXECUTION TEST", dispatcher);
    }
    
}
