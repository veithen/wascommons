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

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class TraceCaptureFactory implements TraceCaptureFactoryMBean {
    private final MBeanServer mbs;
    private final ObjectName rasLoggingService;
    private final ObjectName traceService;

    public TraceCaptureFactory(MBeanServer mbs) throws JMException {
        this.mbs = mbs;
        rasLoggingService = MBeanUtil.findMBean(mbs, new ObjectName("WebSphere:type=RasLoggingService,*"));
        traceService = MBeanUtil.findMBean(mbs, new ObjectName("WebSphere:type=TraceService,*"));
    }

    public MBeanServer getMBeanServer() {
        return mbs;
    }

    public ObjectName getRasLoggingService() {
        return rasLoggingService;
    }

    public ObjectName getTraceService() {
        return traceService;
    }

    public ObjectName createTraceCapture(String name) throws JMException {
        ObjectName objectName = MBeanUtil.formatObjectName(TraceCapture.class, name);
        
        TraceCapture capture = new TraceCapture(this);
        objectName = mbs.registerMBean(capture, objectName).getObjectName();
        capture.setObjectName(objectName);
        
        return objectName;
    }
}
