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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.ibm.ws.orb.GlobalORBFactory;

public final class ORBUtil {
    private ORBUtil() {}
    
    public static void initGlobalORB() {
        if (GlobalORBFactory.globalORB() == null) {
            Properties props = new Properties();
            InputStream in = ORBUtil.class.getClassLoader().getResourceAsStream("orb.properties");
            if (in != null) {
                try {
                    try {
                        props.load(in);
                    } finally {
                        in.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    props.clear();
                }
            }
            if (props.getProperty("org.omg.CORBA.ORBClass") == null) {
                props.setProperty("org.omg.CORBA.ORBClass", "com.ibm.CORBA.iiop.ORB");
            }
            if (props.getProperty("com.ibm.CORBA.Debug.Output") == null) {
                // This prevents the ORB from creating orbtrc files
                props.setProperty("com.ibm.CORBA.Debug.Output", File.separatorChar == '/' ? "/dev/null" : "NUL");
            }
            GlobalORBFactory.init(new String[0], props);
        }
    }
}
