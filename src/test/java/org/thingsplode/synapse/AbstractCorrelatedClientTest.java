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
package org.thingsplode.synapse;

import java.net.URISyntaxException;
import javax.net.ssl.SSLException;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import static org.thingsplode.synapse.AbstractTest.dispatcher;
import org.thingsplode.synapse.proxy.BlockingProxyTest;
import org.thingsplode.synapse.proxy.Dispatcher;
import org.thingsplode.synapse.proxy.EndpointProxy;

/**
 *
 * @author Csaba Tamas
 */
public class AbstractCorrelatedClientTest extends AbstractTest {

    @ClassRule
    public static ExternalResource clientProxy = new ExternalResource() {

        private EndpointProxy epx;

        @Override
        protected void before() {
            try {
                epx = EndpointProxy.create("http://localhost:8080/", Dispatcher.DispatcherPattern.CORRELATED_ASYNC).
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
}
