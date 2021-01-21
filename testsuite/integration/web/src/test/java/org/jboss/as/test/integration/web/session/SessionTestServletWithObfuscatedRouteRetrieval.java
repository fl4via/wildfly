/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.web.session;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import org.wildfly.extension.undertow.Server;

/**
 * Temporary servlet to demonstrate how can the obfuscated route be retrieved via Server service, or encoded via jboss node nmae + server name + MessageDigest.
 *
 * @author Flavia Rainone
 */
@WebServlet(name = "SessionPersistenceServlet", urlPatterns = {"/SessionPersistenceServlet"})
public class SessionTestServletWithObfuscatedRouteRetrieval extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String nodeName = System.getProperty("jboss.node.name");
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update("default-server".getBytes(StandardCharsets.UTF_8));
        final byte[] md5Bytes = md.digest(nodeName.getBytes(StandardCharsets.UTF_8));
        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        // calculates obfuscated route, if run with obfuscate-session-route set to false, this wont be equal to the route
        String routeViaEncoding = new String(encoder.encode(md5Bytes), StandardCharsets.UTF_8);

        ServiceContainer container = CurrentServiceContainer.getServiceContainer();
        ServiceController<Server> server = (ServiceController<Server>) container.getService(ServiceName.of("org", "wildfly", "undertow", "server", "default-server"));
        // retrieves the route, regardless of it being obfuscated or not
        String routeViaServerService = server.getValue().getRoute();

        if(req.getParameter("invalidate") != null) {
            req.getSession().invalidate();
        } else {
            HttpSession session = req.getSession(true);
            Integer val = (Integer) session.getAttribute("val");
            if (val == null) {
                session.setAttribute("val", 0);
                resp.getWriter().print(0);
            } else {
                session.setAttribute("val", ++val);
                resp.getWriter().print(val);
            }
        }
    }

}
