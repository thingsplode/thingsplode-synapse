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
package org.thingsplode.synapse.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.net.ssl.SSLServerSocket;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class NetworkUtil {

    /**
     *
     * @param interfaceName which depending on the operating system can have
     * different values (Linux/Unix examples: eth0, wlan0, etc.)
     * @return the corresponding network interface instance object.
     * @throws SocketException
     */
    public static NetworkInterface getInterfaceByName(String interfaceName) throws SocketException {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netIf : Collections.list(nets)) {
            if (netIf.getName().equalsIgnoreCase(interfaceName)) {
                return netIf;
            } else {
                Enumeration<NetworkInterface> subIfs = netIf.getSubInterfaces();
                for (NetworkInterface subIf : Collections.list(subIfs)) {
                    if (subIf.getName().equalsIgnoreCase(interfaceName)) {
                        return subIf;
                    }
                }
            }
        }
        return null;
    }

    /**
     * It returns the first INET Address available on the given interface,
     * regardless the type of the address (IPV4 or IPV6).
     *
     * @param iface
     * @return the first INET address (IP Address) configured for this network
     * interface. <b>Please note: </b> there's no guarantee that you'll get an
     * IPV4 or IPV6 address. For controlled address definition use the
     * {@link #getInetAddressByInterface(java.net.NetworkInterface, boolean)}
     * method.
     * @throws SocketException
     */
    public static InetAddress getInetAddressByInterface(NetworkInterface iface) throws SocketException {
        if (iface != null) {
            Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                return inetAddress;
            }
            return null;
        } else {
            return null;
        }
    }

    /**
     * It returns the first INET Address available on the given interface. Using
     * this method you can define which type of address you would like to
     * receive (IPV4 or IPV6)
     *
     * @param iface the interface from which you want to read the first
     * available INET address
     * @param ipv4 if true it will return only IPV4 address (if any), if false
     * it will return only IPV6 address
     * @return the IPV4 or IPV6 address available on the interface or null if
     * none is found (or the combination is not found: ip4 is true, but there's
     * no IPV4 address).
     * @throws SocketException
     */
    public static InetAddress getInetAddressByInterface(NetworkInterface iface, boolean ipv4) throws SocketException {
        if (iface != null) {
            Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                if (inetAddress.getAddress().length == 4 && ipv4) {
                    return inetAddress;
                } else if (inetAddress.getAddress().length == 16 && !ipv4) {
                    return inetAddress;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    /**
     *
     * @param interfaceName
     * @return the first INET address found on the interface specified by the
     * <b>interfaceName</b>. NUll if interface by that name can not be found, or
     * if the iface has no address assigned to it.
     * @throws SocketException
     */
    public static InetAddress getInetAddressByInterface(String interfaceName) throws SocketException {
        NetworkInterface iface = getInterfaceByName(interfaceName);
        return getInetAddressByInterface(iface);
    }

    /**
     * It provides a complete description of all available network interfaces in
     * the system as well as the configured INET Addresses.
     *
     * @return
     * @throws SocketException
     */
    public static String getInterfaceDiagnostic() throws SocketException {
        StringBuilder description = new StringBuilder();
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        for (NetworkInterface netIf : Collections.list(nets)) {
            description.append(describeInterface(netIf, false));
            description.append(displaySubInterfaces(netIf));
            description.append("\n");
        }
        return description.toString();
    }

    public static String displaySubInterfaces(NetworkInterface netIf) throws SocketException {
        StringBuilder sb = new StringBuilder();
        Enumeration<NetworkInterface> subIfs = netIf.getSubInterfaces();
        for (NetworkInterface subIf : Collections.list(subIfs)) {
            sb.append(describeInterface(subIf, true));
        }
        return sb.toString();
    }

    public static String describeInterface(NetworkInterface subIf, boolean subinterface) throws SocketException {
        StringBuilder description = new StringBuilder();
        String padding = "";
        if (subinterface) {
            description.append("\t\tSUBINTERFACE");
            description.append("\t\t____________");
            padding = "\t\t";
        }
        description.append(String.format("%sInterface Display name: %s\n", padding, subIf.getDisplayName()));
        description.append(String.format("%sInterface Name: %s\n", padding, subIf.getName()));
        Enumeration<InetAddress> inetAddresses = subIf.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            description.append(String.format("%s\tInetAddress: %s\n", padding, inetAddress));
        }
        description.append(String.format("%s\t\tUp? %s\n", padding, subIf.isUp()));
        description.append(String.format("%s\t\tLoopback? %s\n", padding, subIf.isLoopback()));
        description.append(String.format("%s\t\tPointToPoint? %s\n", padding, subIf.isPointToPoint()));
        description.append(String.format("%s\t\tSupports multicast? %s\n", padding, subIf.supportsMulticast()));
        description.append(String.format("%s\t\tVirtual? %s\n", padding, subIf.isVirtual()));
        description.append(String.format("%s\t\tHardware address: %s\n", padding,
                Arrays.toString(subIf.getHardwareAddress())));
        description.append(String.format("%s\t\tMTU: %s\n", padding, subIf.getMTU()));
        description.append("\n");
        return description.toString();
    }

    /**
     * Returns as a formatted String most of the socket configuration options. A
     * method useful for dump socket data in the log files or detecting network
     * related issues.
     *
     * @param ss Server Socket for which the description is made
     * @return
     */
    public static String describeSocket(ServerSocket ss) {
        StringBuilder sb = new StringBuilder("Server Socket \n");
        sb.append(String.format("Host Address: \t\t [%s] \n", ss.getInetAddress().getHostAddress()));
        sb.append(String.format("Enable Local Port: \t\t [%s] \n", ss.getLocalPort()));
        try {
            sb.append(String.format("Socket Timeout: \t\t [%s] \n", ss.getSoTimeout()));
        } catch (IOException ex) {
            sb.append(String.format("Socket Timeout Retrieval Error: \t\t [%s] \n", ex.getMessage()));
        }
        try {
            sb.append(String.format("Socket Reuse Address: \t\t [%s] \n", ss.getReuseAddress()));
        } catch (SocketException ex) {
            sb.append(String.format("Reuse Addr Retrieval Error: \t\t [%s] \n", ex.getMessage()));
        }
        return sb.toString();
    }

    /**
     * Similar to the {@link #describeSocket(java.net.ServerSocket) } method,
     * but adds to the description the additional SSL information. Possible use
     * case:
     * <br><code> logger.debug(NetworkUtil.describeSSLServerSocket(((SSLServerSocket) serverSocket)));</code><br>
     *
     * @param sslSs SSL Server Socket to be described
     * @return
     */
    public static String describeSSLServerSocket(SSLServerSocket sslSs) {
        StringBuilder sb = new StringBuilder(describeSocket(sslSs));
        sb.append("SSL Properties on socket of class type: ").append(sslSs.getClass().getSimpleName()).append("\n");

        sb.append(String.format("Enable Session Creation: \t\t [%s] \n", sslSs.getEnableSessionCreation()));

        sb.append(String.format("Need Client Auth: \t\t [%s] \n", sslSs.getNeedClientAuth()));
        sb.append(String.format("Use Client Mode: \t\t [%s] \n", sslSs.getUseClientMode()));
        sb.append(String.format("Want Client Auth: \t\t [%s] \n", sslSs.getWantClientAuth()));
        sb.append("\t\t Enabled cipher suites:\n");
        sb.append("\t\t ---------------------:\n");
        for (String cipherSuite : sslSs.getEnabledCipherSuites()) {
            sb.append("\t\t").append(cipherSuite).append("\n");
        }
        sb.append("\n").append("\t\t Enabled protocols:\n");
        sb.append("\t\t -----------------:\n");
        for (String protocol : sslSs.getEnabledProtocols()) {
            sb.append("\t\t").append(protocol).append("\n");
        }
        return sb.toString();

    }

    private NetworkUtil() {
    }

    /**
     *
     * @param networkInterface
     * @param ipv4
     * @return true if network interface is up and an IPv4 or IPv6 address is
     * assigned
     * @throws SocketException
     */
    public static boolean isNetworkInterfaceConfigured(NetworkInterface networkInterface, boolean ipv4) throws SocketException {
        return networkInterface.isUp() && getInetAddressByInterface(networkInterface, ipv4) != null;
    }

    /**
     *
     * @return a list of all network interfaces mac addresses. e.g.
     * aa:bb:cc:dd:ee:00
     * @throws SocketException
     * @see NetworkInterface.getNetworkInterfaces()
     * @see NetworkInterface.getHardwareAddress()
     */
    public static String[] getMacAddresses() throws SocketException {
        List<String> result = new ArrayList<>();
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
        if (nis != null) {
            while (nis.hasMoreElements()) {
                byte[] mac = nis.nextElement().getHardwareAddress();
                if (mac != null && mac.length == 6) {
                    result.add(String.format("%02X:%02X:%02X:%02X:%02X:%02X", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]));
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }
}
