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
package com.googlecode.chainsaw4was.receiver;

import java.net.InetSocketAddress;
import java.util.Properties;

import com.googlecode.chainsaw4was.tunnel.Tunnel;
import com.googlecode.chainsaw4was.tunnel.TunnelSupport;
import com.googlecode.chainsaw4was.tunnel.TunnelingEnabledPlugin;
import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;

public class RemoteRasLoggingReceiver extends RasLoggingReceiver implements TunnelingEnabledPlugin {
    private final TunnelSupport tunnelSupport;
    private String host = "localhost";
    private int port = 9100;
    private String tunnel;
    
    public RemoteRasLoggingReceiver() {
        tunnelSupport = new TunnelSupport(this);
    }

    @Override
    public String getName() {
        String name = super.getName();
        if (name != null && name.length() > 0) {
            return name;
        } else {
            return "WAS @ " + host + ":" + port;
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        String oldName = getName();
        this.host = host;
        String newName = getName();
        if (!oldName.equals(newName)) {
            firePropertyChange("name", oldName, newName);
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        String oldName = getName();
        this.port = port;
        String newName = getName();
        if (!oldName.equals(newName)) {
            firePropertyChange("name", oldName, newName);
        }
    }

    public String getTunnel() {
        return tunnel;
    }

    public void setTunnel(String tunnel) {
        this.tunnel = tunnel;
    }

    @Override
    protected Admin createAdmin() throws Exception {
        ORBUtil.initGlobalORB();
        Properties clientProps = new Properties();
        clientProps.setProperty(AdminClient.CONNECTOR_TYPE, AdminClient.CONNECTOR_TYPE_RMI);
        // TODO: need to close the tunnel somewhere
        Tunnel tunnel = tunnelSupport.createTunnel(host, port);
        if (tunnel == null) {
            clientProps.setProperty(AdminClient.CONNECTOR_HOST, host);
            clientProps.setProperty(AdminClient.CONNECTOR_PORT, String.valueOf(port));
        } else {
            InetSocketAddress localAddress = tunnel.getSocketAddress();
            clientProps.setProperty(AdminClient.CONNECTOR_HOST, localAddress.getHostName());
            clientProps.setProperty(AdminClient.CONNECTOR_PORT, String.valueOf(localAddress.getPort()));
        }
        return new AdminClientWrapper(AdminClientFactory.createAdminClient(clientProps));
    }
}
