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

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.management.JMException;
import javax.management.MBeanServer;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.AdminServiceFactory;

@SuppressWarnings("serial")
public class TraceCaptureStartUpBean implements SessionBean {
    private static final TraceComponent TC = Tr.register(TraceCaptureStartUpBean.class, Constants.TRACE_GROUP, null);
    
    private MBeanServer mbs;
    
    public void ejbCreate() throws CreateException {
    }

    public void ejbRemove() throws EJBException, RemoteException {
    }

    public void setSessionContext(SessionContext sessionContext) throws EJBException, RemoteException {
    }

    public void ejbActivate() throws EJBException, RemoteException {
    }

    public void ejbPassivate() throws EJBException, RemoteException {
    }

    public boolean start() {
        Tr.entry(TC, "start");
        mbs = AdminServiceFactory.getMBeanFactory().getMBeanServer();
        try {
            mbs.registerMBean(new TraceCaptureFactory(mbs), MBeanUtil.formatObjectName(TraceCaptureFactory.class, null));
            Tr.exit(TC, "start");
            return true;
        } catch (JMException ex) {
            Tr.error(TC, "Failed to register MBean: " + ex.getMessage());
            return false;
        }
    }
    
    public void stop() {
        
    }
}
