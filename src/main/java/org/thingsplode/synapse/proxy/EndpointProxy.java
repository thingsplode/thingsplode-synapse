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
import java.util.HashSet;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.proxy.handlers.HttpClientResponseHandler;
import org.thingsplode.synapse.proxy.handlers.HttpResponseIntrospector;
import org.thingsplode.synapse.proxy.handlers.RequestToHttpRequestEncoder;

/**
 *
 * @author Csaba Tamas
 */
//todo: remove this -> http://netty.io/4.0/xref/io/netty/example/http/snoop/package-summary.html
//todo: support for basic authentication (//todo: support for basic authentication )
//todo: dispatching strategy (worker pool, ringbuffer, single threaded)
public class EndpointProxy {

    private Logger logger = LoggerFactory.getLogger(EndpointProxy.class);
    private final URI connectionUri;
    private EventLoopGroup group = new NioEventLoopGroup();
    private SslContext sslContext = null;
    private Bootstrap b = null;
    private final HashSet<Dispatcher> dispatchers = new HashSet<>();
    private boolean introspection = false;

    //todo: place connection uri to the aqcuire dispatcher, so one client instance can work with many servers
    private EndpointProxy(String baseUrl) throws URISyntaxException {
        this.connectionUri = new URI(baseUrl);
    }

    public static final EndpointProxy create(String baseUrl) throws URISyntaxException {
        return new EndpointProxy(baseUrl);
    }

    public EndpointProxy start() {
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
                            p.addLast(new HttpRequestEncoder());
                            p.addLast(new HttpResponseDecoder());
                            p.addLast(new RequestToHttpRequestEncoder());
                            if (introspection) {
                                p.addLast(new HttpResponseIntrospector());
                            }
                            //p.addLast(new HttpContentDecompressor());
                            //todo: chose one of the two and tune it
                            p.addLast(new HttpObjectAggregator(1048576));
                            //p.addLast(new HttpClientHandler());
                            p.addLast(new HttpClientResponseHandler());
                        }
                    });
        } finally {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                this.logger.info("Stopping client.");
                this.stop();
            }));
        }
        return this;

    }

    public EndpointProxy enableIntrospection() {
        this.introspection = true;
        return this;
    }

    public void stop() {
        dispatchers.forEach(d -> {
            try {
                dispatchers.remove(d);
                d.destroy();
            } catch (InterruptedException ex) {
                logger.warn("Interrupted while destroying duspatcher: " + ex.getMessage());
            }
        });
        group.shutdownGracefully();
    }

    public Dispatcher acquireDispatcher() throws SSLException, InterruptedException {
        int port = 0;
        boolean ssl = false;
        if (this.connectionUri.getPort() == -1) {
            if (this.connectionUri.getScheme() != null) {
                if ("http".equalsIgnoreCase(this.connectionUri.getScheme()) || "ws".equalsIgnoreCase(this.connectionUri.getScheme())) {
                    port = 80;
                } else if ("https".equalsIgnoreCase(this.connectionUri.getScheme()) || "wss".equalsIgnoreCase(this.connectionUri.getScheme())) {
                    port = 443;
                    ssl = true;
                } else {
                    port = 8000;
                    ssl = false;
                }
            }
        } else {
            port = this.connectionUri.getPort();
        }
        if ("https".equalsIgnoreCase(this.connectionUri.getScheme()) || "wss".equalsIgnoreCase(this.connectionUri.getScheme())) {
            ssl = true;
        }
        if (ssl) {
            //todo: extends beyond prototype quality
            sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        }

        Channel ch = b.connect(this.connectionUri.getHost(), port).sync().channel();
        Dispatcher d = new Dispatcher(ch, this.connectionUri.getHost() + ":" + this.connectionUri.getPort());
        dispatchers.add(d);
        return d;
    }

}
