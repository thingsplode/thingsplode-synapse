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

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashSet;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.domain.Event;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;
import org.thingsplode.synapse.core.domain.Uri;

/**
 *
 * @author Csaba Tamas
 */
public class Dispatcher {

    private org.slf4j.Logger logger = LoggerFactory.getLogger(Dispatcher.class);
    private final Channel ch;
    private final URI uri;
    private final HashSet<RequestDecorator> decorators;

    public Dispatcher(Channel ch, URI uri) {
        this(ch, uri, null);
    }

    public Dispatcher(Channel ch, URI u, HashSet<RequestDecorator> decorators) {
        this.ch = ch;
        this.uri = u;
        if (decorators != null) {
            this.decorators = decorators;
        } else {
            this.decorators = new HashSet<>();
        }
        this.decorators.add((RequestDecorator) (Request request) -> {
            try {
                //basic data decorator
                request.getHeader().addRequestProperty(HttpHeaderNames.HOST.toString(), this.uri.getHost());
                request.getHeader().addRequestProperty(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.KEEP_ALIVE.toString());
                request.getHeader().addRequestProperty(HttpHeaderNames.ACCEPT_ENCODING.toString(), HttpHeaderValues.GZIP.toString());
                request.getHeader().setUri(new Uri(this.uri.getRawPath()));
            } catch (UnsupportedEncodingException ex) {
                logger.error("Error while configuring default dispatcher decorator: " + ex.getMessage(), ex);
            }
        });
    }

    public void broadcast(Event event) {
        ch.writeAndFlush(event);
    }

    public Response dispatch(Request request) {
        return null;
    }

    public <T> T createStub(String servicePath, Class<T> aClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void destroy() throws InterruptedException {
        // Wait for the server to close the connection.
        ch.closeFuture().sync();
    }
}
