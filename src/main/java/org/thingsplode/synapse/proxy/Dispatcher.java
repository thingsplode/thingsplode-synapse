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
import java.util.HashSet;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;

/**
 *
 * @author Csaba Tamas
 */
public class Dispatcher {

    private org.slf4j.Logger logger = LoggerFactory.getLogger(Dispatcher.class);
    private final Channel ch;
    private final String hostExpression;
    private final HashSet<RequestDecorator> decorators;

    public Dispatcher(Channel ch, String hostExpression) {
        this(ch, hostExpression, null);
    }

    public Dispatcher(Channel ch, String he, HashSet<RequestDecorator> decorators) {
        this.ch = ch;
        this.hostExpression = he;
        if (decorators != null) {
            this.decorators = decorators;
        } else {
            this.decorators = new HashSet<>();
        }
        this.decorators.add((RequestDecorator) (Request request) -> {
            //basic data decorator
            request.getHeader().addRequestProperty(HttpHeaderNames.HOST.toString(), hostExpression);
            request.getHeader().addRequestProperty(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.KEEP_ALIVE.toString());
            request.getHeader().addRequestProperty(HttpHeaderNames.ACCEPT_ENCODING.toString(), HttpHeaderValues.GZIP.toString());
            request.getHeader().addRequestProperty(HttpHeaderNames.ACCEPT.toString(), "*/*");
            request.getHeader().addRequestProperty(HttpHeaderNames.USER_AGENT.toString(), "synapse");
        });
    }

    public void broadcast(Request event) {
        decorate(event);
        ch.writeAndFlush(event);
    }

    public Response dispatch(Request request) {
        decorate(request);
        return null;
    }

    private void decorate(Request req) {
        decorators.forEach(d -> d.decorate(req));
    }

    public <T> T createStub(String servicePath, Class<T> aClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void destroy() throws InterruptedException {
        // Wait for the server to close the connection.
        ch.closeFuture().sync();
    }
}
