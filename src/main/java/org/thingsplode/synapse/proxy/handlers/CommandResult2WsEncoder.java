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
import java.util.List;
import java.util.UUID;
import org.thingsplode.synapse.MessageIdGeneratorStrategy;
import org.thingsplode.synapse.core.CommandResult;
import org.thingsplode.synapse.core.MediaType;
import org.thingsplode.synapse.proxy.EndpointProxy;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
public class CommandResult2WsEncoder extends MessageToMessageEncoder<CommandResult> {
    
    private final MessageIdGeneratorStrategy messageIdGeneratorStrategy;
    
    public CommandResult2WsEncoder(MessageIdGeneratorStrategy messageIdGeneratorStrategy) {
        if (messageIdGeneratorStrategy != null) {
            this.messageIdGeneratorStrategy = messageIdGeneratorStrategy;
        } else {
            this.messageIdGeneratorStrategy = () -> UUID.randomUUID().toString();
        }
    }
    
    @Override
    protected void encode(ChannelHandlerContext ctx, CommandResult msg, List<Object> out) throws Exception {
        if (Util.isEmpty(msg.getHeader().getMsgId())) {
            msg.getHeader().setMsgId(messageIdGeneratorStrategy.getNextId());
        }
        String result = EndpointProxy.SERIALIZATION_SERVICE.getSerializer(MediaType.APPLICATION_JSON).marshallToWireformat(msg);
        out.add(new TextWebSocketFrame(result));
    }
    
}
