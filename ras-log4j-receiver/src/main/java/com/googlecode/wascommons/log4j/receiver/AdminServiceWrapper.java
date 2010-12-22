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

import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;

import com.ibm.websphere.management.AdminService;

public class AdminServiceWrapper implements Admin {
    private final AdminService adminService;

    public AdminServiceWrapper(AdminService adminService) {
        this.adminService = adminService;
    }

    public Set queryNames(ObjectName name, QueryExp query) {
        return adminService.queryNames(name, query);
    }

    public void addNotificationListener(ObjectName name,
            NotificationListener listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        adminService.addNotificationListener(name, listener, filter, handback);
    }

    public void removeNotificationListener(ObjectName name,
            NotificationListener listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        adminService.removeNotificationListener(name, listener);
    }
}
