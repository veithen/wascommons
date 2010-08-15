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
package com.googlecode.chainsaw4was.zeroconf;

import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.LogManager;
import org.apache.log4j.joran.JoranConfigurator;

public class InitServlet extends HttpServlet {
    private static final long serialVersionUID = -7735302844163342602L;

    @Override
    public void init() throws ServletException {
        JoranConfigurator configurator = new JoranConfigurator();
        try {
            configurator.doConfigure(getServletContext().getResource("/WEB-INF/log4j.xml"), LogManager.getLoggerRepository());
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
    }

    @Override
    public void destroy() {
    }
}
