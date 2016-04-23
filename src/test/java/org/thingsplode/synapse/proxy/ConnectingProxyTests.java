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

import java.net.URISyntaxException;
import javax.net.ssl.SSLException;
import org.thingsplode.synapse.AbstractTest;


/**
 *
 * @author Csaba Tamas
 */
public class ConnectingProxyTests extends AbstractTest{

    public void testConnectionRetry() {
        try {
            EndpointProxy epx = EndpointProxy.create("http://localhost:8080/", Dispatcher.DispatcherPattern.BLOCKING_REQUEST).setRetryConnection(true).start();
            dispatcher = epx.acquireDispatcher();
        } catch (URISyntaxException | SSLException | InterruptedException th) {
            System.out.println("\n\n\nERROR while setting up: " + BlockingProxyTest.class.getSimpleName() + ". Dumping stack trace: ");
            th.printStackTrace();
        }
    }
    
    //todo: connecting disconnecting with idle
    //todo: reconnect while connecting for the first time
    //todo: reconnect when unilaterally disconnected
    

}
