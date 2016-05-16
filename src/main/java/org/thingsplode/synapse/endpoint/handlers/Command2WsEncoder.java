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
package org.thingsplode.synapse.endpoint.handlers;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.Command;
import org.thingsplode.synapse.core.MediaType;
import org.thingsplode.synapse.core.PushNotification;
import org.thingsplode.synapse.serializers.SerializationService;

/**
 *
 * @author Csaba Tamas
 */
public class Command2WsEncoder extends MessageToMessageEncoder<Command> {

    private static final Logger logger = LoggerFactory.getLogger(Command2WsEncoder.class);
    private final SerializationService serializationService = SerializationService.getInstance();

    @Override
    protected void encode(ChannelHandlerContext ctx, Command command, List<Object> out) throws Exception {
        if (command instanceof PushNotification){
            ((PushNotification)command).setDefaultTopicIfNone();
        }
        byte[] content = serializationService.getSerializer(MediaType.APPLICATION_JSON).marshall(command);
        TextWebSocketFrame wsFrame = new TextWebSocketFrame(Unpooled.wrappedBuffer(content));
        out.add(wsFrame);
    }

}
