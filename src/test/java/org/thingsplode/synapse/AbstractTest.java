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
package org.thingsplode.synapse;

import com.acme.synapse.testdata.services.CrudTestEndpointService;
import com.acme.synapse.testdata.services.DummyMarkedEndpoint;
import com.acme.synapse.testdata.services.EndpointTesterService;
import io.netty.handler.logging.LogLevel;
import java.net.InetSocketAddress;
import org.junit.rules.ExternalResource;
import org.thingsplode.synapse.endpoint.Endpoint;
import org.thingsplode.synapse.endpoint.Endpoint.ConnectionProvider;
import com.acme.synapse.testdata.services.RpcEndpointImpl;
import java.io.FileNotFoundException;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.proxy.Dispatcher;

/**
 *
 * @author Csaba Tamas
 */
public abstract class AbstractTest {

    private static Logger logger = LoggerFactory.getLogger(AbstractTest.class);
    protected static Dispatcher dispatcher;
    @ClassRule
    public static ExternalResource synapticEndpoint = new ExternalResource() {
        private Endpoint ep;

        @Override
        protected void before() throws InterruptedException, FileNotFoundException {
            System.out.println("\n\n BEFORE METHOD CALLED\n\n");

            Thread t = new Thread(() -> {
                try {
                    ep = Endpoint.create("test", new ConnectionProvider(new InetSocketAddress("0.0.0.0", 8080)))
                            .logLevel(LogLevel.TRACE)
                            .protocol(Endpoint.Protocol.JSON)
                            .transportType(Endpoint.TransportType.HTTP_REST)
                            .enableFileHandler(System.getProperty("java.io.tmpdir"))
                            .enableSwagger("1.0", null)
                            .enabledWebsocket()
                            .enableIntrospection()
                            .enabledWebsocket()
                            .publish(new RpcEndpointImpl())
                            .publish(new EndpointTesterService())
                            .publish(new CrudTestEndpointService())
                            .publish(new DummyMarkedEndpoint())
                            .publish("/default/", new TestEventProcessor());
                    ep.start();
                } catch (InterruptedException | FileNotFoundException ex) {
                    logger.error("Error while initializing test: " + ex.getMessage(), ex);
                }
            });
            t.start();
            Thread.sleep(1000);
        }

        @Override
        protected void after() {
            if (ep != null) {
                ep.stop();
            }
        }

        public Endpoint getEp() {
            return ep;
        }
    };
}

//Todo: remove the content from bellow
//http://netty.io/wiki/user-guide-for-4.x.html#wiki-h3-11
//https://keyholesoftware.com/2015/03/16/netty-a-different-kind-of-websocket-server/
//https://github.com/jwboardman/khs-stockticker/blob/master/src/main/java/com/khs/stockticker/StockTickerServer.java
//https://github.com/jwboardman/khs-stockticker/blob/master/src/main/java/com/khs/stockticker/NettyHttpFileHandler.java
//http://microservices.io/patterns/microservices.html
