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

import java.util.Iterator;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class MBeanUtil {
    private MBeanUtil() {}
    
    public static ObjectName formatObjectName(Class<?> clazz, String name) throws MalformedObjectNameException {
        StringBuilder buffer = new StringBuilder("com.googlecode.wascommons:type=");
        buffer.append(clazz.getSimpleName());
        if (name != null) {
            buffer.append(",name=");
            buffer.append(name);
        }
        return new ObjectName(buffer.toString());
    }
    
    public static ObjectName findMBean(MBeanServer mbs, ObjectName queryName) throws JMException {
        Iterator<ObjectName> it = mbs.queryMBeans(queryName, null).iterator();
        if (it.hasNext()) {
            ObjectName name = it.next();
            if (it.hasNext()) {
                throw new JMException("Found more than one MBean matching " + queryName);
            } else {
                return name;
            }
        } else {
            throw new JMException("Failed to locate " + queryName);
        }
    }
}
