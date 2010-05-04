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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.ULogger;
import org.apache.log4j.plugins.Receiver;
import org.apache.log4j.spi.LoggingEvent;

import com.ibm.ejs.ras.RasMessageImpl2;
import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.NotificationConstants;

public class RasLoggingReceiver extends Receiver implements NotificationListener {
    private static final Map<String,Level> rasTypeToLevelMap = new HashMap<String,Level>();
    private static final Field localizedMessageField;
    
    static {
        rasTypeToLevelMap.put(NotificationConstants.TYPE_RAS_INFO, Level.INFO);
        rasTypeToLevelMap.put(NotificationConstants.TYPE_RAS_AUDIT, Level.INFO);
        rasTypeToLevelMap.put(NotificationConstants.TYPE_RAS_SERVICE, Level.INFO);
        rasTypeToLevelMap.put(NotificationConstants.TYPE_RAS_WARNING, Level.WARN);
        rasTypeToLevelMap.put(NotificationConstants.TYPE_RAS_ERROR, Level.ERROR);
        rasTypeToLevelMap.put(NotificationConstants.TYPE_RAS_FATAL, Level.FATAL);
        try {
            localizedMessageField = RasMessageImpl2.class.getDeclaredField("ivLocalizedMessage");
        } catch (NoSuchFieldException ex) {
            throw new NoSuchFieldError(ex.getMessage());
        }
        localizedMessageField.setAccessible(true);
    }
    
    private String host = "localhost";
    private int port = 9100;
    private AdminClient adminClient;
    private final List<ObjectName> rasMBeans = new ArrayList<ObjectName>();
    private int instanceCount;
    
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
    
    public int getInstanceCount() {
        return instanceCount;
    }
    
    private void updateInstanceCount(int delta) {
        int oldInstanceCount = instanceCount;
        instanceCount++;
        firePropertyChange("instanceCount", oldInstanceCount, instanceCount);
    }

    public void activateOptions() {
        ULogger log = getLogger();
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
                updateInstanceCount(1);
                log.info("Started listening to " + rasMBean);
                rasMBeans.add(rasMBean);
            }
        } catch (Throwable ex) {
            log.error("Couldn't start " + RasLoggingReceiver.class.getName(), ex);
        }
    }
    
    public void handleNotification(Notification notification, Object handback) {
        RasMessageImpl2 message = (RasMessageImpl2)notification.getUserData();
        // We extract the localized message using reflection because
        // getLocalizedMessage will always compare the locale. If there is a
        // locale mismatch and the necessary resource bundle is not found,
        // no message will be returned.
        String localizedMessage;
        try {
            localizedMessage = (String)localizedMessageField.get(message);
        } catch (IllegalAccessException ex) {
            throw new IllegalAccessError(ex.getMessage());
        }
        Logger logger = getLoggerRepository().getLogger(message.getMessageOriginator());
        LoggingEvent event = new LoggingEvent(logger.getName(), logger,
                rasTypeToLevelMap.get(notification.getType()),
                localizedMessage, null);
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
        ULogger log = getLogger();
        for (ObjectName rasMBean : rasMBeans) {
            try {
                adminClient.removeNotificationListener(rasMBean, this);
                updateInstanceCount(-1);
                log.info("Stopped listening to " + rasMBean);
            } catch (Throwable ex) {
                log.error("Couldn't remove notification listener for " + rasMBean, ex);
            }
        }
    }
}
