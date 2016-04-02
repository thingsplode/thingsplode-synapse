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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.endpoint.handlers.HttpFileHandler;
import org.thingsplode.synapse.endpoint.handlers.HttpRequestIntrospector;
import org.thingsplode.synapse.endpoint.handlers.HttpRequestHandler;
import org.thingsplode.synapse.endpoint.handlers.HttpResponseHandler;
import org.thingsplode.synapse.endpoint.handlers.RequestHandler;
import org.thingsplode.synapse.endpoint.swagger.EndpointApiGenerator;
import org.thingsplode.synapse.util.NetworkUtil;

/**
 *
 * @author Csaba Tamas //todo: add basic authorization
 */
public class Endpoint {

    private Logger logger = LoggerFactory.getLogger(Endpoint.class);
    public static final String ENDPOINT_URL_PATERN = "/services|/services/";
    public static final String HTTP_ENCODER = "http_encoder";
    public static final String HTTP_DECODER = "http_decoder";
    public static final String REQUEST_HANDLER = "request_handler";
    public static final String ALL_CHANNEL_GROUP_NAME = "all-open-channels";
    public static final String HTTP_FILE_HANDLER = "http_file_handler";
    public static final String HTTP_RESPONSE_HANDLER = "http_response_handler";
    private static int TERMINATION_TIMEOUT = 60;//number of seconds to wait after the worker threads are shutted down and until those are finishing the last bit of the execution.
    private EventLoopGroup masterGroup = null;//the eventloop group used for the server socket
    private EventLoopGroup workerGroup = null;//the event loop group used for the connected clients
    private final ChannelGroup channelRegistry = new DefaultChannelGroup(ALL_CHANNEL_GROUP_NAME, GlobalEventExecutor.INSTANCE);//all active channels are listed here
    private String endpointId;
    private ConnectionProvider connections;
    private LogLevel logLevel;
    private final ServerBootstrap bootstrap = new ServerBootstrap();
    private TransportType transportType = TransportType.DOMAIN_SOCKET;
    private Protocol protocol = Protocol.JSON;
    private Lifecycle lifecycle = Lifecycle.UNITIALIZED;
    private HttpFileHandler fileHandler = null;
    private EventExecutorGroup evtExecutorGroup = new DefaultEventExecutorGroup(10);
    private ServiceRegistry serviceRegistry = new ServiceRegistry();
    private EndpointApiGenerator apiGenerator = null;
    private boolean introspection = false;

    private enum Lifecycle {
        UNITIALIZED,
        STARTED
    }

    private Endpoint() {
    }

    private Endpoint(String id, ConnectionProvider connections) {
        this.endpointId = id;
        this.connections = connections;
    }

    /**
     *
     * @param endpointId
     * @param connections
     * @return
     * @throws java.lang.InterruptedException
     */
    public static Endpoint create(String endpointId, ConnectionProvider connections) throws InterruptedException {
        Endpoint ep = new Endpoint(endpointId, connections);
        return ep;
    }

    public void start() throws InterruptedException {
        try {
            this.initGroups();
            this.bootstrap.group(this.masterGroup, this.workerGroup);
            if (transportType.equals(TransportType.HTTP_REST)) {
                this.bootstrap.
                        channel(NioServerSocketChannel.class)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline p = ch.pipeline();
                                p.addLast(HTTP_ENCODER, new HttpResponseEncoder());
                                p.addLast(HTTP_DECODER, new HttpRequestDecoder());
                                p.addLast("aggregator", new HttpObjectAggregator(1048576));
                                if (introspection) {
                                    p.addLast(new HttpRequestIntrospector());
                                }
                                p.addLast(evtExecutorGroup, "http_request_handler", new HttpRequestHandler(endpointId, serviceRegistry));
                                if (fileHandler != null) {
                                    p.addLast(evtExecutorGroup, HTTP_FILE_HANDLER, fileHandler);
                                }
                                p.addLast(HTTP_RESPONSE_HANDLER, new HttpResponseHandler());
                                p.addLast(evtExecutorGroup, "handler", new RequestHandler());
                            }
                        });
            } else {
                this.bootstrap.channel(EpollServerDomainSocketChannel.class).childHandler(new ChannelInitializer<DomainSocketChannel>() {
                    @Override
                    protected void initChannel(DomainSocketChannel ch) throws Exception {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                });
            }
            this.bootstrap.handler(new LoggingHandler(logLevel)).option(ChannelOption.SO_BACKLOG, 3);
        } catch (Exception ex) {
            this.logger.error(Endpoint.class.getSimpleName() + " interrupted due to: " + ex.getMessage(), ex);
        } finally {
            this.logger.debug("Adding shutdown hook for the endpoint.");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                this.logger.info("Stopping endpoint [{}].", endpointId);
                this.stop();
            }));
        }
        this.startInternal();
    }

    /**
     *
     * @throws InterruptedException
     */
    private void startInternal() throws InterruptedException {
        lifecycle = Lifecycle.STARTED;
        for (SocketAddress addr : connections.getSocketAddresses()) {
            ChannelFuture channelFuture = bootstrap.bind(addr).sync();
            Channel channel = channelFuture.await().channel();
            channelRegistry.add(channel);
            channelFuture.channel().closeFuture().sync();
        }

    }

    public void stop() {
        lifecycle = Lifecycle.UNITIALIZED;
        channelRegistry.close().awaitUninterruptibly();
        if (masterGroup != null) {
            logger.debug("Closing down Master Group event-loop gracefully...");
            masterGroup.shutdownGracefully(1, TERMINATION_TIMEOUT, TimeUnit.SECONDS);
        }
        if (workerGroup != null) {
            logger.debug("Closing down Worker Group event-loop gracefully...");
            workerGroup.shutdownGracefully(5, TERMINATION_TIMEOUT, TimeUnit.SECONDS);
        }
    }

    private void initGroups() throws InterruptedException {
        if (masterGroup == null && workerGroup == null) {
            masterGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            return;
        }

        if (workerGroup.isShuttingDown()) {
            logger.debug("Worker Event Loop Group is awaiting termination.");
            workerGroup.awaitTermination(TERMINATION_TIMEOUT, TimeUnit.SECONDS);
        }

        if (masterGroup.isShuttingDown()) {
            logger.debug("Master Event Loop Group is awaiting termination.");
            masterGroup.awaitTermination(TERMINATION_TIMEOUT, TimeUnit.SECONDS);
        }

        if (masterGroup.isShutdown()) {
            logger.debug("Creating new Master Event Loop Group.");
            masterGroup = new NioEventLoopGroup();
        } else {
            logger.debug("Master Event Loop Group will not be reinitialized because it is not yet shut down.");
        }
        if (workerGroup.isShutdown()) {
            logger.debug("Creating new Worker Event Loop Group.");
            workerGroup = new NioEventLoopGroup();
        } else {
            logger.debug("Worker Event Loop Group will not be reinitialized because it is not yet shut down.");
        }
    }

    public Endpoint publish(String path, Object serviceInstance) {
        //        if (lifecycle == Lifecycle.UNITIALIZED){
        //            throw new IllegalStateException();
        //        }
        serviceRegistry.register(path, serviceInstance);
        return this;
    }

    public Endpoint publish(Object serviceInstance) {
        if (apiGenerator != null && !(serviceInstance instanceof EndpointApiGenerator)) {
            apiGenerator.addPackageToBeScanned(serviceInstance.getClass().getPackage().getName());
        }
        this.publish(null, serviceInstance);
        return this;
    }

    public enum Protocol {
        JSON
    }

    public enum TransportType {

        /**
         * Default
         */
        HTTP_REST,
        /**
         *
         */
        WEBSOCKET_STOMP,
        /**
         *
         */
        MQTT,
        /**
         * Unix Domain Socket - useful for host only IPC style communication
         */
        DOMAIN_SOCKET;
    }

    public static class ConnectionProvider {

        private final Set<SocketAddress> socketAddresses;

        public ConnectionProvider(int port, String... interfaceNames) {
            this.socketAddresses = new HashSet<>();
            for (String iface : interfaceNames) {
                InetAddress inetAddress;
                try {
                    inetAddress = NetworkUtil.getInetAddressByInterface(NetworkUtil.getInterfaceByName(iface), NetworkUtil.IPVersionFilter.BOTH);
                } catch (SocketException ex) {
                    throw new RuntimeException(ex);
                }
                if (inetAddress == null) {
                    throw new RuntimeException("No internet address bound to network interface " + iface);
                } else {
                    socketAddresses.add(new InetSocketAddress(inetAddress, port));
                }

            }
        }

        public ConnectionProvider(SocketAddress... addresses) {
            if (addresses == null) {
                throw new RuntimeException("Please specify some socket addresses.");
            }
            this.socketAddresses = new HashSet<>(Arrays.asList(addresses));
        }

        public Set<SocketAddress> getSocketAddresses() {
            return socketAddresses;
        }
    }

    public Endpoint enableIntrospection() {
        this.introspection = true;
        return this;
    }

    /**
     *
     * @param version
     * @param hostname the host name (ip address) under the endpoints are
     * available. If null, the first IPV4 address will be taken from the first
     * configured non-loopback interface.
     * @return
     * @throws FileNotFoundException
     */
    public Endpoint enableSwagger(String version, String hostname) throws FileNotFoundException {

        if (fileHandler == null) {
            fileHandler = new HttpFileHandler();
        }
        fileHandler.addRedirect(Pattern.compile(ENDPOINT_URL_PATERN), "/services/index.html?jurl=http://{Host}/services/json");
        if (hostname == null) {
            Optional<String> firstHost = connections.getSocketAddresses().stream().map(sa -> {
                if (sa instanceof InetSocketAddress) {
                    String hostName = ((InetSocketAddress) sa).getHostName();
                    return (hostName.equalsIgnoreCase("0.0.0.0") ? NetworkUtil.getFirstConfiguredHostAddress().get() : hostName) + ":" + ((InetSocketAddress) sa).getPort();
                } else {
                    return "";
                }
            }).findFirst();
            hostname = firstHost.get();
        }
        apiGenerator = new EndpointApiGenerator(version, hostname);
        this.publish(apiGenerator);
        return this;
    }

    public Endpoint enableFileHandler(String webroot) throws FileNotFoundException {
        if (fileHandler == null) {
            fileHandler = new HttpFileHandler(webroot);
        } else {
            fileHandler.setWebroot(webroot);
        }
        return this;
    }

    public Endpoint logLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public Endpoint transportType(TransportType transportType) {
        this.transportType = transportType;
        return this;
    }

    public Endpoint protocol(Protocol protocol) {
        this.protocol = protocol;
        return this;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public TransportType getTransportType() {
        return transportType;
    }
}
