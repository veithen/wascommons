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
package com.googlecode.wascommons.tracecapture;

import java.util.Locale;
import java.util.regex.Pattern;

import javax.management.JMException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import com.ibm.websphere.ras.RasMessage;

public class TraceCapture implements TraceCaptureMBean, NotificationListener {
    private final TraceCaptureFactory factory;
    private ObjectName objectName;
    private boolean enabled;
    private String category;
    private Pattern pattern;
    
    public TraceCapture(TraceCaptureFactory factory) {
        this.factory = factory;
    }

    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean enabled) throws JMException {
        if (enabled != this.enabled) {
            if (enabled) {
                if (category == null && pattern == null) {
                    throw new JMException("You must at least specify one criterium");
                }
                factory.getMBeanServer().addNotificationListener(factory.getRasLoggingService(), this, null, null);
            } else {
                factory.getMBeanServer().removeNotificationListener(factory.getRasLoggingService(), this);
            }
            this.enabled = enabled;
        }
    }
    
    public synchronized String getCategory() {
        return category;
    }

    public synchronized void setCategory(String category) {
        this.category = category == null || category.length() == 0 ? null : category;
    }

    public synchronized String getPattern() {
        return pattern == null ? null : pattern.toString();
    }

    public synchronized void setPattern(String pattern) {
        if (pattern == null || pattern.length() == 0) {
            this.pattern = null;
        } else {
            this.pattern = Pattern.compile(pattern);
        }
    }
    
    public void handleNotification(Notification notification, Object handback) {
        String category;
        Pattern pattern;
        synchronized (this) {
            category = this.category;
            pattern = this.pattern;
        }
        RasMessage message = (RasMessage)notification.getUserData();
        if ((category == null || message.getMessageOriginator().equals(category))
                && (pattern == null || pattern.matcher(message.getLocalizedMessage(Locale.getDefault())).matches())) {
            
        }
    }
}
