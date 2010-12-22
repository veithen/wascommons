/*
 * Copyright 2010 Andreas Veithen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.wascommons.log4j.tunnel.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.ULogger;

import com.googlecode.wascommons.log4j.tunnel.Tunnel;
import com.googlecode.wascommons.log4j.tunnel.TunnelException;
import com.jcraft.jsch.ChannelDirectTCPIP;
import com.jcraft.jsch.Session;

public class SshTunnel implements Tunnel, Runnable {
    private final Session session;
    private final String host;
    private final int port;
    private final ULogger log;
    private final ServerSocket serverSocket;

    public SshTunnel(Session session, String host, int port, ULogger log) throws TunnelException {
        this.session = session;
        this.host = host;
        this.port = port;
        this.log = log;
        try {
            // TODO: specify bind address (localhost)
            serverSocket = new ServerSocket(0);
        } catch (IOException ex) {
            throw new TunnelException("Unable to create server socket", ex);
        }
    }

    public InetSocketAddress getSocketAddress() {
        return new InetSocketAddress("localhost", serverSocket.getLocalPort());
    }

    public void close() throws TunnelException {
        try {
            serverSocket.close();
        } catch (IOException ex) {
            throw new TunnelException("Error closing server socket", ex);
        }
    }

    public void run() {
        int lport = serverSocket.getLocalPort();
        log.info("Created tunnel " + lport + " to " + host + ":" + port);
        while (true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException ex) {
                return;
            }
            try {
                socket.setTcpNoDelay(true);
                ChannelDirectTCPIP channel = (ChannelDirectTCPIP)session.openChannel("direct-tcpip");
                channel.setInputStream(socket.getInputStream());
                channel.setOutputStream(socket.getOutputStream());
                channel.setHost(host);
                channel.setPort(port);
                channel.setOrgIPAddress(socket.getInetAddress().getHostAddress());
                channel.setOrgPort(socket.getPort());
                channel.connect();
                log.info("Established connection to " + host + ":" + port + " through tunnel " + lport);
            } catch (Exception ex) {
                log.error("Failed to establish connection to " + host + ":" + port + " through tunnel " + lport, ex);
            }
        }
    }
}
