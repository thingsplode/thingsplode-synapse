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
package org.thingsplode.synapse.endpoint;

import io.netty.handler.logging.LogLevel;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import org.apache.log4j.BasicConfigurator;

/**
 *
 * @author Csaba Tamas
 */
public class EndpointBootstrap {

    private Endpoint ep;

    public EndpointBootstrap() throws InterruptedException, FileNotFoundException {
        BasicConfigurator.configure();
        this.ep = Endpoint.create("test", new Endpoint.ConnectionProvider(new InetSocketAddress("0.0.0.0", 8080)))
                .logLevel(LogLevel.TRACE)
                .protocol(Endpoint.Protocol.JSON)
                .transportType(Endpoint.TransportType.HTTP_REST)
                .enableFileHandler(System.getProperty("java.io.tmpdir"))
                .enableSwagger("1.0", null);
//                        .publish(new RpcEndpointImpl())
//                        .publish(new EndpointTesterService())
//                        .publish(new CrudTestEndpointService())
//                        .publish(new DummyMarkedEndpoint())
//                        .publish("/default/", new AbstractEventSink<Address>(Address.class) {
//                            @Override
//                            protected void eventReceived(Event<Address> event) {
//                                System.out.println("Event Received: " + event.getBody());
//                            }
//                        });
        this.ep.start();
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        EndpointBootstrap endpointExampleMain = new EndpointBootstrap();
    }

}
