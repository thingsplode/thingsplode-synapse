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
package org.thingsplode.synapse.core;

import io.netty.channel.ChannelHandlerContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author Csaba Tamas
 */
public class ConnectionContext {

    private final InetAddress remoteInetAddress;
    private final String remoteHost;
    private final int remotePort;
    private final ChannelHandlerContext ctx;
    private long lastSeen;
    private String clientID;
    private boolean authorized;
    private final HashMap<String,String> properties = new HashMap<>();

    public ConnectionContext(ChannelHandlerContext ctx) {
        this.remoteInetAddress = getAddress(ctx).getAddress();
        this.remoteHost = remoteInetAddress.getHostAddress() != null ? remoteInetAddress.getHostAddress() : "N/A";
        this.remotePort = getAddress(ctx).getPort();
        this.ctx = ctx;
        updateLastSeen();
    }

    /**
     * A null-safe utility method to extract the {@link InetSocketAddress}
     * associated with a {@link Channel}
     *
     * @param ctx the {@link ChannelHandlerContext} on which the channel can be
     * found
     * @return the socket address associated with the channel. Null if the
     * channel or the {@link Channel#remoteAddress() } returns null;
     */
    public final static InetSocketAddress getAddress(ChannelHandlerContext ctx) {
        return ctx.channel() != null
                ? ctx.channel().remoteAddress() != null
                        ? ((InetSocketAddress) ctx.channel().remoteAddress())
                        : null
                : null;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    /**
     * Sets the {@link System#currentTimeMillis() } to the {@link #lastSeen}
     * attribute; This method is mostly used internally, to update the last-seen
     * date any time a wallbox communicates with the server.
     */
    public final void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    /**
     * @return the remoteInetAddress associated with the communication channel
     * (address of the wallbox). This will return a value even the channel is
     * closed in the meantime.
     */
    public InetAddress getRemoteInetAddress() {
        return remoteInetAddress;
    }

    /**
     * @return the remoteHost associated with the communication channel. (host
     * name of the wallbox) This will return a value even the channel is closed
     * in the meantime.
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * @return the remotePort associated with the communication channel. (port
     * which is used by the wallbox to initiated communication) This will return
     * a value even the channel is closed in the meantime.
     */
    public int getRemotePort() {
        return remotePort;
    }

    /**
     * @return the lastSeen, which is a Unix Time representation (milliseconds
     * retreived by {@link System#currentTimeMillis() }) representing the last
     * time the wallbox was communicating something.
     */
    public long getLastSeen() {
        return lastSeen;
    }

    /**
     * @return the authorized status. If true, the wallbox is authorized to
     * communicate to the {@link WallboxReceiver}, otherwise it still need to
     * execute a {@link Messages.ConnectionAuthorizationReq} message.
     */
    public boolean isAuthorized() {
        return authorized;
    }

    /**
     * @param authorized the authorized to set (if set to true, the connecting
     * wallbox will be able to send various messages without the connection
     * being shutdown).
     */
    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }
    
    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public Optional<String> getProperty(String propertyKey) {
        return Optional.ofNullable(properties.get(propertyKey));
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    public void addAllProperties(Iterable<Map.Entry<String, String>> iterable) {
        if (iterable != null) {
            iterable.forEach((e) -> properties.put(e.getKey(), e.getValue()));
        }
    }

}
