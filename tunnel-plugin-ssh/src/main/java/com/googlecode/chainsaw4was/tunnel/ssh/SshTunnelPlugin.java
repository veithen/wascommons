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

import org.apache.log4j.plugins.PluginSkeleton;

import com.googlecode.chainsaw4was.tunnel.Tunnel;
import com.googlecode.chainsaw4was.tunnel.TunnelException;
import com.googlecode.chainsaw4was.tunnel.TunnelPlugin;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshTunnelPlugin extends PluginSkeleton implements TunnelPlugin {
    private String host;
    private int port = 22;
    private String user;
    private String password;
    private Session session;
    
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void activateOptions() {
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setUserInfo(new InteractiveUserInfo());
            session.setPassword(password);
            session.connect();
        } catch (JSchException ex) {
            getLogger().error("Unable to create SSH session", ex);
        }
    }
    
    public void shutdown() {
        session.disconnect();
    }
    
    public Tunnel createTunnel(String host, int port) throws TunnelException {
        SshTunnel tunnel = new SshTunnel(session, host, port, getLogger());
        new Thread(tunnel).start();
        return tunnel;
    }
}
