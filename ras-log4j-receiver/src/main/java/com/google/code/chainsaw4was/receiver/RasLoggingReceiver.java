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
package com.google.code.chainsaw4was.receiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.plugins.Receiver;
import org.apache.log4j.spi.LoggingEvent;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.NotificationConstants;
import com.ibm.websphere.ras.RasMessage;

public class RasLoggingReceiver extends Receiver implements NotificationListener {
    private static final Map<String,Level> rasTypeToLevelMap = new HashMap<String,Level>();
    
    static {
        rasTypeToLevelMap.put(NotificationConstants.TYPE_RAS_INFO, Level.INFO);
        rasTypeToLevelMap.put(NotificationConstants.TYPE_RAS_AUDIT, Level.INFO);
        rasTypeToLevelMap.put(NotificationConstants.TYPE_RAS_SERVICE, Level.INFO);
        rasTypeToLevelMap.put(NotificationConstants.TYPE_RAS_WARNING, Level.WARN);
        rasTypeToLevelMap.put(NotificationConstants.TYPE_RAS_ERROR, Level.ERROR);
        rasTypeToLevelMap.put(NotificationConstants.TYPE_RAS_FATAL, Level.FATAL);
    }
    
    private String host = "localhost";
    private int port = 9100;
    private AdminClient adminClient;
    private final List<ObjectName> rasMBeans = new ArrayList<ObjectName>();
    
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

    public void activateOptions() {
        try {
            Properties clientProps = new Properties();
            clientProps.setProperty(AdminClient.CONNECTOR_TYPE, AdminClient.CONNECTOR_TYPE_RMI);
            clientProps.setProperty(AdminClient.CONNECTOR_HOST, host);
            clientProps.setProperty(AdminClient.CONNECTOR_PORT, String.valueOf(port));
            adminClient = AdminClientFactory.createAdminClient(clientProps);
            ObjectName queryMBean = new ObjectName("WebSphere:type=RasLoggingService,*");
            for (Iterator it = adminClient.queryNames(queryMBean, null).iterator(); it.hasNext(); ) {
                ObjectName rasMBean = (ObjectName)it.next(); 
                /*
                NotificationFilterSupport filter = new NotificationFilterSupport();
                filter.enableType(NotificationConstants.TYPE_RAS_FATAL);
                filter.enableType(NotificationConstants.TYPE_RAS_ERROR);
                filter.enableType(NotificationConstants.TYPE_RAS_WARNING);
                filter.enableType(NotificationConstants.TYPE_RAS_INFO);
                filter.enableType(NotificationConstants.TYPE_RAS_AUDIT);
                filter.enableType(NotificationConstants.TYPE_RAS_SERVICE);*/
                adminClient.addNotificationListener(rasMBean, this, null, rasMBean);
                rasMBeans.add(rasMBean);
            }
        } catch (Throwable ex) {
            getLogger().error("Couldn't start " + RasLoggingReceiver.class.getName(), ex);
        }
    }
    
    public void handleNotification(Notification notification, Object handback) {
        RasMessage message = (RasMessage)notification.getUserData();
        Logger logger = getLoggerRepository().getLogger(message.getMessageOriginator());
        LoggingEvent event = new LoggingEvent(logger.getName(), logger,
                rasTypeToLevelMap.get(notification.getType()),
                message.getLocalizedMessage(Locale.ENGLISH), null);
        event.setTimeStamp(message.getTimeStamp());
        event.setThreadName(message.getThreadId());
        ObjectName rasMBean = (ObjectName)handback;
        String cell = rasMBean.getKeyProperty("cell");
        String node = rasMBean.getKeyProperty("node");
        String process = rasMBean.getKeyProperty("process");
        // Predefined properties
        event.setProperty("hostname", cell + "/" + node);
        event.setProperty("application", process);
        event.setProperty("log4jid", Long.toString(notification.getSequenceNumber()));
        // Custom properties
        event.setProperty("cell", cell);
        event.setProperty("node", node);
        event.setProperty("process", process);
        event.setProperty("version", rasMBean.getKeyProperty("version"));
        event.setProperty("severity", message.getMessageSeverity());
        doPost(event);
    }

    public void shutdown() {
        for (ObjectName rasMBean : rasMBeans) {
            try {
                adminClient.removeNotificationListener(rasMBean, this);
            } catch (Throwable ex) {
                getLogger().error("Couldn't remove notification listener for " + rasMBean, ex);
            }
        }
    }
}
