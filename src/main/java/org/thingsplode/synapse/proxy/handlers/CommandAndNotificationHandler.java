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

import org.thingsplode.synapse.proxy.AbstractCommandSink;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.Command;
import org.thingsplode.synapse.core.CommandResult;
import org.thingsplode.synapse.core.PushNotification;
import org.thingsplode.synapse.proxy.AbstractNotificationSink;
import org.thingsplode.synapse.proxy.EndpointProxy;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
public class CommandAndNotificationHandler extends SimpleChannelInboundHandler<Command> {

    private final Logger logger = LoggerFactory.getLogger(CommandAndNotificationHandler.class);
    private final AbstractCommandSink commandSink;
    private AbstractNotificationSink defaultNotificationSink;
    private final HashMap<String, AbstractNotificationSink> subscription;

    public CommandAndNotificationHandler(AbstractCommandSink commandSink, HashMap<String, AbstractNotificationSink> subscription) {

        if (subscription != null) {
            this.subscription = subscription;
        } else {
            this.subscription = new HashMap<>();
        }

        if (!this.subscription.containsKey(EndpointProxy.DEFAULT_NOTIFICATION_TOPIC)) {
            this.subscription.put(EndpointProxy.DEFAULT_NOTIFICATION_TOPIC, new AbstractNotificationSink() {
                @Override
                public void onMessage(PushNotification notification) {
                    logger.warn("There's no default notification sink configured, therefore notifications are getting ignored: " + notification.toString());
                }
            });
        }

        this.defaultNotificationSink = this.subscription.get(EndpointProxy.DEFAULT_NOTIFICATION_TOPIC);

        if (commandSink != null) {
            this.commandSink = commandSink;
        } else {
            this.commandSink = new AbstractCommandSink() {
                @Override
                public CommandResult execute(Command command) {
                    CommandResult cr = new CommandResult(command, CommandResult.ResultCode.REJECTED);
                    cr.getHeader().setErrorReason("No default command sink is registered.");
                    return cr;
                }
            };
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        if (msg instanceof PushNotification) {
            PushNotification n = (PushNotification) msg;
            if (!Util.isEmpty(n.getHeader().getSourceTopic())) {
                defaultNotificationSink.onMessage(n);
            } else {
                AbstractNotificationSink nsink = subscription.get(n.getHeader().getSourceTopic());
                if (nsink == null) {
                    defaultNotificationSink.onMessage(n);
                } else {
                    nsink.onMessage(n);
                }
            }
        } else {
            ctx.writeAndFlush(commandSink.processCommand(msg));
        }
    }

}
