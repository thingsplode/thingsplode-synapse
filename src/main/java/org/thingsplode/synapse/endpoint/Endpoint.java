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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.endpoint.handlers.HttRequestHandler;
import org.thingsplode.synapse.endpoint.handlers.RequestHandler;
import org.thingsplode.synapse.util.NetworkUtil;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class Endpoint {

    private Logger logger = LoggerFactory.getLogger(Endpoint.class);
    public static final String HTTP_ENCODER = "http_encoder";
    public static final String HTTP_DECODER = "http_decoder";
    public static final String REQUEST_HANDLER = "request_handler";
    public static final String ALL_CHANNEL_GROUP_NAME = "all-open-channels";
    private static int TERMINATION_TIMEOUT = 60;//number of seconds to wait after the worker threads are shutted down and until those are finishing the last bit of the execution.
    private EventLoopGroup masterGroup = null;//the eventloop group used for the server socket
    private EventLoopGroup workerGroup = null;//the event loop group used for the connected clients
    private final ChannelGroup channelRegistry = new DefaultChannelGroup(ALL_CHANNEL_GROUP_NAME, GlobalEventExecutor.INSTANCE);//all active channels are listed here
    private String endpointId;
    private Connections connections;
    private LogLevel logLevel;
    private final ServerBootstrap bootstrap = new ServerBootstrap();
    private TransportType transportType = TransportType.DOMAIN_SOCKET;
    private Protocol protocol = Protocol.JSON;
    private Lifecycle lifecycle = Lifecycle.UNITIALIZED;
    private EventExecutorGroup evtExecutorGroup = new DefaultEventExecutorGroup(10);

    private enum Lifecycle {
        UNITIALIZED,
        STARTED
    }

    private Endpoint() {
    }

    private Endpoint(String id, Connections connections) {
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
    public static Endpoint create(String endpointId, Connections connections) throws InterruptedException {
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
                                p.addLast("aggregator", new HttpObjectAggregator(65536));
                                p.addLast("http_request_handler", new HttRequestHandler(endpointId, null));
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
                this.logger.info("Stopping endpoint [%s].", endpointId);
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
        for (SocketAddress addr : connections.getSocketAddresses()) {
            ChannelFuture channelFuture = bootstrap.bind(addr).sync();
            Channel channel = channelFuture.await().channel();
            channelRegistry.add(channel);
        }
        lifecycle = Lifecycle.STARTED;
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

    public Endpoint publish(String serviceID, Object serviceInstance) {
//        if (lifecycle == Lifecycle.UNITIALIZED){
//            throw new IllegalStateException();
//        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

    public static class Connections {

        private final Set<SocketAddress> socketAddresses;

        public Connections(int port, String... interfaceNames) {
            this.socketAddresses = new HashSet<>();
            for (String iface : interfaceNames) {
                InetAddress inetAddress;
                try {
                    inetAddress = NetworkUtil.getInetAddressByInterface(NetworkUtil.getInterfaceByName(iface), true);
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

        public Connections(SocketAddress... addresses) {
            if (addresses == null) {
                throw new RuntimeException("Please specify some socket addresses.");
            }
            this.socketAddresses = new HashSet<>(Arrays.asList(addresses));
        }

        public Set<SocketAddress> getSocketAddresses() {
            return socketAddresses;
        }
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
