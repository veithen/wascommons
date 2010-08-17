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
package com.googlecode.chainsaw4was.tunnel;

import java.util.Iterator;

import org.apache.log4j.plugins.Plugin;
import org.apache.log4j.plugins.PluginRegistry;
import org.apache.log4j.spi.LoggerRepositoryEx;

public class TunnelSupport {
    private final TunnelingEnabledPlugin plugin;
    
    public TunnelSupport(TunnelingEnabledPlugin plugin) {
        this.plugin = plugin;
    }
    
    public Tunnel createTunnel(String host, int port) throws TunnelException {
        String tunnelName = plugin.getTunnel();
        if (tunnelName != null && tunnelName.length() > 0) {
            PluginRegistry pluginRegistry = ((LoggerRepositoryEx)plugin.getLoggerRepository()).getPluginRegistry();
            TunnelPlugin tunnelPlugin = null;
            for (Iterator it = pluginRegistry.getPlugins().iterator(); it.hasNext(); ) {
                Plugin candidate = (Plugin)it.next();
                if (candidate.getName().equals(tunnelName) && candidate instanceof TunnelPlugin) {
                    tunnelPlugin = (TunnelPlugin)candidate;
                    break;
                }
            }
            if (tunnelPlugin == null) {
                throw new TunnelException("No such tunnel: " + tunnelName);
            }
            return tunnelPlugin.createTunnel(host, port);
        } else {
            return null;
        }
    }
}
