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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import org.thingsplode.synapse.core.domain.Request;
import org.thingsplode.synapse.core.domain.Response;

/**
 * It will order into a FIFO order the responses for message pipelining (see HTTP pipelining).
 * In case Response B (response for Request B) is available earlier than Response A, it will be stored in a waiting queue, until Response A
 * is not dispatched.
 *
 * *Work In Progress*
 * @author Csaba Tamas
 * 
 * Todo: (once it is ready is should support sequencing / client and state sharing among node must be assured as well.)
 */
public class ResponseSequencer extends MessageToMessageEncoder<Response> {

    private final AtomicLong deliveredSequence = new AtomicLong(0);
    private final SortedSet<Response> responses = new TreeSet<>(new ResponseComparator());

    @Override
    protected void encode(ChannelHandlerContext ctx, Response msg, List<Object> out) throws Exception {
        Optional<String> sequence = msg.getHeaderProperty(Request.RequestHeader.MSG_SEQ);
        if (sequence.isPresent()) {
            long currentValue = Long.parseLong(sequence.get());
            if (currentValue == deliveredSequence.get()) {
                out.add(msg);
                return;
            } else {
                
            }
        }
    }

    private class ResponseComparator implements Comparator<Response> {

        @Override
        public int compare(Response x, Response y) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
