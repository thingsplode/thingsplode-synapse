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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.ComponentLifecycle;
import org.thingsplode.synapse.endpoint.Endpoint;
import org.thingsplode.synapse.proxy.handlers.HttpResponse2ResponseDecoder;
import org.thingsplode.synapse.proxy.handlers.InboundExceptionHandler;
import org.thingsplode.synapse.proxy.handlers.HttpResponseIntrospector;
import org.thingsplode.synapse.proxy.handlers.RequestEncoder;
import org.thingsplode.synapse.proxy.handlers.Request2HttpRequestEncoder;
import org.thingsplode.synapse.proxy.handlers.Request2WsRequestEncoder;
import org.thingsplode.synapse.proxy.handlers.ResponseHandler;
import org.thingsplode.synapse.proxy.handlers.WSResponse2ResponseDecoder;
import org.thingsplode.synapse.serializers.SerializationService;

/**
 *
 * @author Csaba Tamas
 */
//todo: support for basic authentication (//todo: support for basic authentication )
//todo: dispatching strategy (worker pool, ringbuffer, single threaded)
//todo: port already binded
public class EndpointProxy {

    public final static SerializationService SERIALIZATION_SERVICE = SerializationService.getInstance();
    public final static String HTTP_REQUEST_ENCODER = "HTTP_REQUEST_ENCODER";
    public final static String HTTP_RESPONSE_DECODER = "HTTP_RESPONSE_DECODER";
    public final static String HTTP_RESPONSE_AGGREGATOR = "HTTP_RESPONSE_AGGREGATOR";
    public final static String REQUEST2HTTP_REQUEST_ENCODER = "REQUEST2HTTP_REQUEST_ENCODER";
    public final static String REQUEST2WS_REQUEST_ENCODER = "REQUEST2WS_REQUEST_ENCODER";
    public final static String HTTP_RSP2RESPONSE_DECODER = "HTTP_RSP2RESPONSE_DECODER";
    public final static String WS_RESPONSE_DECODER = "WS_RESPONSE_DECODER";
    private final Logger logger = LoggerFactory.getLogger(EndpointProxy.class);
    private final URI connectionUri;
    private final EventLoopGroup group = new NioEventLoopGroup();
    private SslContext sslContext = null;
    private Bootstrap b = null;
    private final HashSet<Dispatcher> dispatchers = new HashSet<>();
    private boolean introspection = false;
    private int connectTimeout = 3000;
    private ComponentLifecycle lifecycle = ComponentLifecycle.UNITIALIZED;
    private final Dispatcher.DispatcherPattern dispatchPattern;
    private DispatchedFutureHandler dfh;
    private boolean retryConnection = false;
    private final RequestEncoder requestEncoder;
    private HttpResponseIntrospector httpResponseIntrospector = new HttpResponseIntrospector();
    private final ResponseHandler responseHandler;
    private final InboundExceptionHandler inboundExceptionHandler;
    private MessageIdGeneratorStrategy msgIdGeneratorStrategy;
    private Transport transport;
//todo: place connection uri to the aqcuire dispatcher, so one client instance can work with many servers

    private EndpointProxy(String baseUrl, Dispatcher.DispatcherPattern dispatchPattern) throws URISyntaxException {
        this.connectionUri = new URI(baseUrl);
        this.transport = getTransport(this.connectionUri);
        this.dispatchPattern = dispatchPattern;
        switch (this.dispatchPattern) {
            case BLOCKING_REQUEST:
                dfh = new BlockingRspCorrelator();
                break;
            case CORRELATED_ASYNC:
                dfh = new MsgIdRspCorrelator();
                break;
            case PIPELINING:
                //todo: dfh = ?;
                throw new UnsupportedOperationException("Method not supported yet.");
            //break;
        }
        this.requestEncoder = new RequestEncoder(null, connectionUri.getHost() + ":" + connectionUri.getPort());
        this.inboundExceptionHandler = new InboundExceptionHandler(dfh);
        this.responseHandler = new ResponseHandler(dfh);
    }

    public static final EndpointProxy create(String baseUrl, Dispatcher.DispatcherPattern dispatchPattern) throws URISyntaxException {
        return new EndpointProxy(baseUrl, dispatchPattern);
    }

    public EndpointProxy initialize() {
        b = new Bootstrap();
        try {
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            if (sslContext != null) {
                                p.addLast(sslContext.newHandler(ch.alloc()));
                            }
                            //todo: tune the values here
                            //p.addLast(new HttpClientCodec());
                            //p.addLast(new HttpContentDecompressor());
                            p.addLast(HTTP_REQUEST_ENCODER, new HttpRequestEncoder());
                            p.addLast(HTTP_RESPONSE_DECODER, new HttpResponseDecoder());
                            p.addLast(HTTP_RESPONSE_AGGREGATOR, new HttpObjectAggregator(1048576));
                            if (introspection) {
                                p.addLast(httpResponseIntrospector);
                            }
                            switch (transport.transportType) {
                                case HTTP: {
                                    p.addLast(REQUEST2HTTP_REQUEST_ENCODER, new Request2HttpRequestEncoder());
                                    break;
                                }
                                case WEBSOCKET: {
                                    p.addLast(REQUEST2WS_REQUEST_ENCODER, new Request2WsRequestEncoder());
                                    break;
                                }
                                default:
                                    logger.warn("No handler is supporting yet the following transport: " + transport.transportType);
                            }

                            p.addLast(requestEncoder);
                            switch (transport.transportType) {
                                case HTTP: {
                                    p.addLast(HTTP_RSP2RESPONSE_DECODER, new HttpResponse2ResponseDecoder());
                                    break;
                                }
                                case WEBSOCKET: {
                                    p.addLast(WS_RESPONSE_DECODER, new WSResponse2ResponseDecoder(connectionUri));
                                    break;
                                }
                                default:
                                    logger.warn("No response handler is supporting yet the following transport: " + transport.transportType);
                            }
                            p.addLast(responseHandler);
                            p.addLast(inboundExceptionHandler);
                        }
                    });
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        } finally {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                this.logger.info("Activating endpoint proxy (client) shutdown hook...");
                this.stop();
                ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS);
            }));
        }
        this.lifecycle = ComponentLifecycle.INITIALIZED;
        return this;

    }

    /**
     *
     * @param timeoutInMillis the default is 3000
     * @return
     */
    public EndpointProxy setConnectTimeout(int timeoutInMillis) {
        if (this.lifecycle == ComponentLifecycle.INITIALIZED) {
            throw new IllegalStateException("Please set this value before starting the " + EndpointProxy.class.getSimpleName());
        }
        this.connectTimeout = timeoutInMillis;
        return this;
    }

    public EndpointProxy msgIdGeneratorStrategy(MessageIdGeneratorStrategy strategy) {
        this.msgIdGeneratorStrategy = strategy;
        return this;
    }

    /**
     *
     * @param retryConnection if set to true, the dispatcher will try to
     * reconnect indefinitely if the server is not available.
     * @return
     */
    public EndpointProxy setRetryConnection(boolean retryConnection) {
        if (this.lifecycle == ComponentLifecycle.INITIALIZED) {
            throw new IllegalStateException("Please set this value before starting the " + EndpointProxy.class.getSimpleName());
        }
        this.retryConnection = retryConnection;
        return this;
    }

    public EndpointProxy enableIntrospection() {
        if (this.lifecycle == ComponentLifecycle.INITIALIZED) {
            throw new IllegalStateException("Please set this value before starting the " + EndpointProxy.class.getSimpleName());
        }
        this.introspection = true;
        return this;
    }

    public void stop() {
        if (this.lifecycle == ComponentLifecycle.UNITIALIZED) {
            throw new IllegalStateException("Please use this method after starting the " + EndpointProxy.class.getSimpleName());
        }
        dispatchers.forEach(d -> {
            try {
                dispatchers.remove(d);
                d.destroy();
            } catch (InterruptedException ex) {
                logger.warn("Interrupted while destroying duspatcher: " + ex.getMessage());
            }
        });
        group.shutdownGracefully();
        this.lifecycle = ComponentLifecycle.UNITIALIZED;
    }

    public Dispatcher acquireDispatcher() throws SSLException, InterruptedException {
        if (this.lifecycle == ComponentLifecycle.UNITIALIZED) {
            throw new IllegalStateException("Please set this value before starting the " + EndpointProxy.class.getSimpleName());
        }

        int port = this.connectionUri.getPort() == -1 ? this.transport.getSchemaDefaultPort() : this.connectionUri.getPort();

        if (transport.ssl) {
            //todo: extends beyond prototype quality
            sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        }
        //final int dirtyTrickPort = port; //needed because for some reason the compiler does not accept port and unitialized final int, even if there's an else in the if statement above.
        if (this.msgIdGeneratorStrategy == null) {
            this.msgIdGeneratorStrategy = () -> UUID.randomUUID().toString();
        }
        Dispatcher dispatcher = new Dispatcher(retryConnection, dfh, msgIdGeneratorStrategy, b, this.connectionUri.getHost(), port);
        dispatcher.connect();
        dispatchers.add(dispatcher);
        return dispatcher;
    }

    private class Transport {

        Endpoint.TransportType transportType;
        boolean ssl;

        public Transport(Endpoint.TransportType transportType, boolean ssl) {
            this.transportType = transportType;
            this.ssl = ssl;
        }

        int getSchemaDefaultPort() {
            switch (transportType) {
                case HTTP:
                    return ssl ? 443 : 80;
                case WEBSOCKET:
                    return ssl ? 443 : 80;
                case MQTT:
                    return ssl ? 8883 : 1883;
                default:
                    return 8000;
            }
        }
    }

    private Transport getTransport(URI uri) {
        switch (uri.getScheme()) {
            case "http": {
                return new Transport(Endpoint.TransportType.HTTP, false);
            }
            case "https": {
                return new Transport(Endpoint.TransportType.HTTP, true);
            }
            case "ws": {
                return new Transport(Endpoint.TransportType.WEBSOCKET, false);
            }
            case "wss": {
                return new Transport(Endpoint.TransportType.WEBSOCKET, true);
            }
            case "mqtt": {
                return new Transport(Endpoint.TransportType.MQTT, false);
            }
            case "mqtts": {
                return new Transport(Endpoint.TransportType.MQTT, true);
            }
            case "file": {
                return new Transport(Endpoint.TransportType.DOMAIN_SOCKET, false);
            }
            default: {
                throw new UnsupportedAddressTypeException();
            }

        }
    }

}
