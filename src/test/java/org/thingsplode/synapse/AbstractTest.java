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

import io.netty.handler.logging.LogLevel;
import java.net.InetSocketAddress;
import org.apache.log4j.BasicConfigurator;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.thingsplode.synapse.endpoint.Endpoint;
import org.thingsplode.synapse.endpoint.Endpoint.Connections;
import com.acme.synapse.testdata.services.RpcEndpointImpl;
import com.acme.synapse.testdata.services.RpcEndpoint;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public abstract class AbstractTest {

    private Endpoint ep;

    @Rule
    public ExternalResource resource;

    public AbstractTest() {
        this.resource = new ExternalResource() {

            private Endpoint ep;

            @Override
            protected void before() throws InterruptedException {
                System.out.println("\n\n BEFORE METHOD CALLED\n\n");
                RpcEndpoint remoteService = new RpcEndpointImpl();
                Connections c;
                BasicConfigurator.configure();
                this.ep = Endpoint.create("test", new Connections(new InetSocketAddress("0.0.0.0", 8080)))
                        .logLevel(LogLevel.TRACE)
                        .protocol(Endpoint.Protocol.JSON)
                        .transportType(Endpoint.TransportType.HTTP_REST);
                        //.publish("test_service", remoteService);
                this.ep.start();
            }

            @Override
            protected void after() {
                ep.stop();
            }
        };
    }

    public Endpoint getEndpoint() {
        return ep;
    }
}

//http://netty.io/wiki/user-guide-for-4.x.html#wiki-h3-11
//https://keyholesoftware.com/2015/03/16/netty-a-different-kind-of-websocket-server/
//https://github.com/jwboardman/khs-stockticker/blob/master/src/main/java/com/khs/stockticker/StockTickerServer.java
//https://github.com/jwboardman/khs-stockticker/blob/master/src/main/java/com/khs/stockticker/NettyHttpFileHandler.java
//http://microservices.io/patterns/microservices.html