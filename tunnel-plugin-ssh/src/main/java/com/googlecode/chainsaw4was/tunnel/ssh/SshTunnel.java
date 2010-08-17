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
package com.googlecode.chainsaw4was.tunnel.ssh;

import java.net.InetSocketAddress;

import com.googlecode.chainsaw4was.tunnel.Tunnel;
import com.googlecode.chainsaw4was.tunnel.TunnelException;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshTunnel implements Tunnel {
    private final Session session;
    private final int localPort;

    public SshTunnel(Session session, int localPort) {
        this.session = session;
        this.localPort = localPort;
    }

    public InetSocketAddress getSocketAddress() {
        return new InetSocketAddress("localhost", localPort);
    }

    public void close() throws TunnelException {
        try {
            session.delPortForwardingL(localPort);
        } catch (JSchException ex) {
            throw new TunnelException("Unable to delete port forwarding", ex);
        }
    }
}
