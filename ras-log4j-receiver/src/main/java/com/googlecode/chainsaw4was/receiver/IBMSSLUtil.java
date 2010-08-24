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
package com.googlecode.chainsaw4was.receiver;

import java.net.URL;

public final class IBMSSLUtil {
    private IBMSSLUtil() {}
    
    private static boolean initialized;
    
    public static synchronized void init() {
        if (!initialized) {
            URL url = IBMSSLUtil.class.getClassLoader().getResource("ssl.client.props");
            if (url != null) {
                System.setProperty("com.ibm.SSL.ConfigURL", url.toExternalForm());
            }
            initialized = true;
        }
    }
}
