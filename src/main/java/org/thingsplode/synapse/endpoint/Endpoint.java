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

import org.thingsplode.synapse.core.ComponentLifecycle;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.Command;
import org.thingsplode.synapse.core.CommandResult;
import org.thingsplode.synapse.core.ConnectionContext;
import org.thingsplode.synapse.endpoint.handlers.FileRequestHandler;
import org.thingsplode.synapse.endpoint.handlers.HttpRequestIntrospector;
import org.thingsplode.synapse.endpoint.handlers.HttpRequestHandler;
import org.thingsplode.synapse.endpoint.handlers.HttpResponseHandler;
import org.thingsplode.synapse.endpoint.handlers.ResponseIntrospector;
import org.thingsplode.synapse.endpoint.handlers.RequestHandler;
import org.thingsplode.synapse.endpoint.swagger.EndpointApiGenerator;
import org.thingsplode.synapse.DispatchedFuture;
import org.thingsplode.synapse.MessageIdGeneratorStrategy;
import org.thingsplode.synapse.MessageRegistry;
import org.thingsplode.synapse.MsgIdRspCorrelator;
import static org.thingsplode.synapse.endpoint.handlers.RequestHandler.CONNECTION_CTX_ATTR;
import org.thingsplode.synapse.util.NetworkUtil;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas //todo: add basic authorization
 */
public class Endpoint {

    private Logger logger = LoggerFactory.getLogger(Endpoint.class);
    public static final String ENDPOINT_URL_PATERN = "/services|/services/";
    public static final String HTTP_ENCODER = "http_encoder";
    public static final String HTTP_DECODER = "http_decoder";
    public static final String HTTP_AGGREGATOR = "http_aggregator";
    public static final String HTTP_REQUEST_HANDLER = "http_request_handler";
    public static final String REQUEST_HANDLER = "request_handler";
    public static final String ALL_CHANNEL_GROUP_NAME = "all-open-channels";
    public static final String HTTP_FILE_HANDLER = "http_file_handler";
    public static final String RESPONSE_SEQUENCER = "response_sequencer";
    public static final String HTTP_RESPONSE_HANDLER = "http_response_handler";
    public static final String WS_RESPONSE_HANDLER = "ws_response_handler";
    public static final String WS_COMMAND_HANDLER = "WS_COMMAND_HANDLER";
    public static final String WS_REQUEST_HANDLER = "ws_request_handler";
    public static final String RESPONSE_INTROSPECTOR = "RESPONSE_INTROSPECTOR";
    public static final String HTTP_REQUEST_INTROSPECTOR = "HTTP_REQUEST_INTROSPECTOR";
    public static final String WS_REQUEST_INTROSPECTOR = "WS_REQUEST_INTROSPECTOR";
    private static int TERMINATION_TIMEOUT = 60;//number of seconds to wait after the worker threads are shutted down and until those are finishing the last bit of the execution.

    private EventLoopGroup masterGroup = null;//the eventloop group used for the server socket
    private EventLoopGroup workerGroup = null;//the event loop group used for the connected clients
    private final ChannelGroup channelRegistry = new DefaultChannelGroup(ALL_CHANNEL_GROUP_NAME, GlobalEventExecutor.INSTANCE);//all active channels are listed here
    private String endpointId;
    private ConnectionProvider connections;
    private LogLevel logLevel;
    private final ServerBootstrap bootstrap = new ServerBootstrap();
    private List<Transport> transportTypes = new ArrayList<>();
    private DataFormat protocol = DataFormat.JSON;
    private ComponentLifecycle lifecycle = ComponentLifecycle.UNITIALIZED;
    private FileRequestHandler fileHandler = null;
    private EventExecutorGroup evtExecutorGroup = new DefaultEventExecutorGroup(10);
    private ServiceRegistry serviceRegistry = new ServiceRegistry();
    private EndpointApiGenerator apiGenerator = null;
    private boolean introspection = false;
    private boolean pipelining = false;
    private MsgIdRspCorrelator messageStore = null;
    private MessageIdGeneratorStrategy msgIdGeneratorStrategy = null;
    private boolean bidirectionalCommsEnabled = false;
    //private final LinkedBlockingQueue<DispatchedFuture<Command, CommandResult>> dispatchQueue = new LinkedBlockingQueue<>(20);//todo: will 20 be enough

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

    public Endpoint start() throws InterruptedException {
        try {
            logger.debug("Starting endpoint [" + endpointId + "].");

            this.initGroups();
            this.bootstrap.group(this.masterGroup, this.workerGroup);
            boolean ws = transportTypes.contains(Transport.WEBSOCKET);
            //if bidirectional communication is permitted /add more protocols here later
            bidirectionalCommsEnabled = ws;
            if (bidirectionalCommsEnabled) {
                if (this.messageStore == null) {
                    this.messageStore = new MsgIdRspCorrelator();
                }
                if (this.msgIdGeneratorStrategy == null) {
                    this.msgIdGeneratorStrategy = () -> UUID.randomUUID().toString();
                }
            }
            if (transportTypes.contains(Transport.HTTP) || ws) {
                this.bootstrap.
                        channel(NioServerSocketChannel.class)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline p = ch.pipeline();
                                p.addLast(HTTP_ENCODER, new HttpResponseEncoder());
                                p.addLast(HTTP_DECODER, new HttpRequestDecoder());
                                p.addLast(HTTP_AGGREGATOR, new HttpObjectAggregator(1048576));
                                if (introspection) {
                                    p.addLast(RESPONSE_INTROSPECTOR, new ResponseIntrospector());
                                    p.addLast(HTTP_REQUEST_INTROSPECTOR, new HttpRequestIntrospector());
                                }
                                p.addLast(HTTP_REQUEST_HANDLER, new HttpRequestHandler(endpointId, pipelining));
                                //p.addLast(evtExecutorGroup, HTTP_REQUEST_HANDLER, new HttpRequestHandler(endpointId, pipelining, websocketSupport));
                                p.addLast(REQUEST_HANDLER, new RequestHandler(serviceRegistry, channelRegistry));
                                if (fileHandler != null) {
                                    p.addLast(evtExecutorGroup, HTTP_FILE_HANDLER, fileHandler);
                                }
                                //todo: add pipelining
//                                if (pipelining) {
//                                    p.addLast(RESPONSE_SEQUENCER, new ResponseSequencer());
//                                }
                                p.addLast(HTTP_RESPONSE_HANDLER, new HttpResponseHandler());
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
                this.logger.info("Endpoint shutdown hook activated [{}].", endpointId);
                this.stop();
                ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS);
            }));
        }
        this.startInternal();
        return this;
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
            //channelFuture.channel().closeFuture().sync();
        }
        lifecycle = ComponentLifecycle.INITIALIZED;
        logger.info("Endpoint [" + endpointId + "] started.");
    }

    public void stop() {
        if (lifecycle == ComponentLifecycle.UNITIALIZED) {
            logger.warn("Endpoint is still/already uninitialized. Exiting stop method...");
            return;
        }
        logger.debug("Stopping endpoint [" + endpointId + "].");
        lifecycle = ComponentLifecycle.UNITIALIZED;
        channelRegistry.close().awaitUninterruptibly();
        if (masterGroup != null) {
            logger.debug("Closing down Master Group event-loop gracefully...");
            masterGroup.shutdownGracefully(1, TERMINATION_TIMEOUT, TimeUnit.SECONDS);
        }
        if (workerGroup != null) {
            logger.debug("Closing down Worker Group event-loop gracefully...");
            workerGroup.shutdownGracefully(5, TERMINATION_TIMEOUT, TimeUnit.SECONDS);
        }
        logger.info("Endpoint [" + endpointId + "] stopped.");
        lifecycle = ComponentLifecycle.UNITIALIZED;
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

    public enum DataFormat {
        JSON
    }

    public enum Transport {
        /**
         * Default
         */
        HTTP,
        /**
         *
         */
        WEBSOCKET,
        /**
         *
         */
        MQTT,
        /**
         * Unix Domain Socket - useful for host only IPC style communication
         */
        DOMAIN_SOCKET;
    }

    public List<DispatchedFuture> broadcast(Command command, long timeToLive) {
        ArrayList<DispatchedFuture> dispatches = new ArrayList<>();
        this.channelRegistry.stream().forEach((Channel ch) -> {
            if (ch != null) {
                ConnectionContext connCtx = ch.attr(CONNECTION_CTX_ATTR).get();
                if (connCtx != null) {
                    dispatches.add(dispatchCommand(connCtx, command, timeToLive));
                }
            }
        });
        return dispatches;
    }

    public List<DispatchedFuture> broadcast(List<String> clientIds, Command command, long timeToLive) {
        HashSet<String> clientIdList = new HashSet<>(clientIds);
        ArrayList<DispatchedFuture> dispatches = new ArrayList<>();
        this.channelRegistry.parallelStream().forEach((Channel ch) -> {
            ConnectionContext connCtx = ch.attr(CONNECTION_CTX_ATTR).get();
            if (connCtx != null && !Util.isEmpty(connCtx.getClientID()) && clientIdList.remove(connCtx.getClientID())) {
                dispatches.add(dispatchCommand(connCtx, command, timeToLive));
            }
        });
        return dispatches;
    }

    public DispatchedFuture<Command, CommandResult> dispatchCommand(ConnectionContext connection, Command command, long timeToLive) {
        if (!bidirectionalCommsEnabled) {
            throw new UnsupportedOperationException("Dispatching from the endpoint is only supported with protocols which are supporting bidirectional communication (eg. websocket, mqtt). Please enable such protocol if you need dispatching throug them.");
        }
        command.getHeader().setMsgId(msgIdGeneratorStrategy.getNextId());
        DispatchedFuture<Command, CommandResult> dispatcherFuture = new DispatchedFuture<>(command, connection.getCtx().channel(), timeToLive);
        if (lifecycle == ComponentLifecycle.UNITIALIZED) {
            dispatcherFuture.completeExceptionally(new IllegalStateException("The endpoing is not initialized."));
            return dispatcherFuture;
        } else {
            messageStore.beforeDispatch(dispatcherFuture);
            ChannelFuture cf = connection.getCtx().channel().writeAndFlush(command);
            cf.addListener((ChannelFutureListener) (ChannelFuture future) -> {
                if (!future.isSuccess()) {
                    //the message could not be sent
                    //todo: build a retry mechanism?
                    messageStore.responseReceived(command.getHeader().getMsgId());
                    dispatcherFuture.completeExceptionally(future.cause());
                    future.channel().close();
                } else if (logger.isTraceEnabled()) {
                    logger.trace(command.getClass().getSimpleName() + " message is succesfully dispatched with Msg. Id.: " + command.getHeader().getMsgId());
                }
            });
        }
        return dispatcherFuture;
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

    public Endpoint enablePipelining() {
        this.pipelining = true;
        throw new UnsupportedOperationException("Not implemented yet");
        //return this;
    }

    public Endpoint setMessageResgistry(MessageRegistry msgRegistry) {
        this.messageStore = new MsgIdRspCorrelator(msgRegistry);
        return this;
    }

    public Endpoint setMsgIdGeneratorStrategy(MessageIdGeneratorStrategy messageIdGeneratorStrategy) {
        this.msgIdGeneratorStrategy = messageIdGeneratorStrategy;
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
            fileHandler = new FileRequestHandler();
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
            fileHandler = new FileRequestHandler(webroot);
        } else {
            fileHandler.setWebroot(webroot);
        }
        return this;
    }

    public Endpoint logLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public Endpoint addTransport(Transport transportType) {
        this.transportTypes.add(transportType);
        return this;
    }

    public Endpoint dataFormat(DataFormat protocol) {
        this.protocol = protocol;
        return this;
    }

    public DataFormat getProtocol() {
        return protocol;
    }

    public boolean containsTransportType(Transport transportType) {
        Optional<Transport> ttOpt = this.transportTypes.stream().filter(tt -> tt == transportType).findFirst();
        return ttOpt.isPresent();
    }

    public List<Transport> getTransportType() {
        return transportTypes;
    }
}
