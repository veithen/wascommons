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
package com.googlecode.wascommons.log4j.receiver;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.ULogger;
import org.apache.log4j.plugins.Receiver;
import org.apache.log4j.spi.LoggingEvent;

import com.ibm.ejs.ras.RasMessageImpl2;
import com.ibm.websphere.management.NotificationConstants;

public abstract class RasLoggingReceiver extends Receiver implements NotificationListener {
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
    
    private int keepAlive;
    private Admin admin;
    private Timer timer;
    private final List<ObjectName> rasMBeans = new ArrayList<ObjectName>();
    private int instanceCount;
    
    public int getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    public synchronized int getInstanceCount() {
        return instanceCount;
    }
    
    private synchronized void updateInstanceCount(int delta) {
        int oldInstanceCount = instanceCount;
        instanceCount += delta;
        firePropertyChange("instanceCount", oldInstanceCount, instanceCount);
    }

    protected abstract Admin createAdmin() throws Exception;
    
    public void activateOptions() {
        final ULogger log = getLogger();
        try {
            admin = createAdmin();
            if (keepAlive > 0) {
                log.debug("Creating keep-alive timer");
                timer = new Timer("RasLoggingReceiver keep alive timer");
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            updateListeners();
                        } catch (Exception ex) {
                            log.error("Keep alive failed", ex);
                        }
                    }
                }, 0, keepAlive*1000);
            } else {
                updateListeners();
            }
        } catch (Throwable ex) {
            log.error("Couldn't start " + getClass().getName(), ex);
        }
    }
    
    private void updateListeners() throws Exception {
        ULogger log = getLogger();
        log.debug("Starting to update RasLoggingService notification listeners");
        Set<ObjectName> unseenRasMBeans = new HashSet<ObjectName>(rasMBeans);
        ObjectName queryMBean = new ObjectName("WebSphere:type=RasLoggingService,*");
        for (ObjectName rasMBean : admin.queryNames(queryMBean, null)) {
            if (!unseenRasMBeans.remove(rasMBean)) {
                NotificationFilterSupport filter;
                Level threshold = getThreshold();
                if (threshold == null) {
                    filter = null;
                } else {
                    filter = new NotificationFilterSupport();
                    for (Map.Entry<String,Level> entry : rasTypeToLevelMap.entrySet()) {
                        if (entry.getValue().isGreaterOrEqual(getThreshold())) {
                            filter.enableType(entry.getKey());
                        }
                    }
                }
                admin.addNotificationListener(rasMBean, this, filter, rasMBean);
                updateInstanceCount(1);
                log.info("Started listening to " + rasMBean);
                rasMBeans.add(rasMBean);
            }
        }
        for (ObjectName rasMBean : unseenRasMBeans) {
            // No need to actually remove the notification listener because this would
            // result in an InstanceNotFoundException anyway.
            log.info(rasMBean + " disappeared.");
            rasMBeans.remove(rasMBean);
            updateInstanceCount(-1);
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
            if (timer != null) {
                timer.cancel();
            }
            try {
                admin.removeNotificationListener(rasMBean, this);
                updateInstanceCount(-1);
                log.info("Stopped listening to " + rasMBean);
            } catch (Throwable ex) {
                log.error("Couldn't remove notification listener for " + rasMBean, ex);
            }
        }
    }
}
