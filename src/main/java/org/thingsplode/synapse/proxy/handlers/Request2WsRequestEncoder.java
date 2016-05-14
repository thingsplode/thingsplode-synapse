/*
 * Copyright 2016 Csaba Tamas.
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
package org.thingsplode.synapse.proxy.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import java.util.List;
import org.thingsplode.synapse.core.MediaType;
import org.thingsplode.synapse.core.Request;
import org.thingsplode.synapse.core.exceptions.SerializationException;
import org.thingsplode.synapse.proxy.EndpointProxy;

/**
 *
 * @author Csaba Tamas
 */
public class Request2WsRequestEncoder extends MessageToMessageEncoder<Request> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Request msg, List<Object> out) throws Exception {
        WebSocketFrame frame = convert(msg);
        out.add(frame);
    }

    private WebSocketFrame convert(Request request) throws SerializationException {
        String msg = EndpointProxy.SERIALIZATION_SERVICE.getSerializer(MediaType.APPLICATION_JSON).marshallToWireformat(request);
        return new TextWebSocketFrame(msg);
    }

}
