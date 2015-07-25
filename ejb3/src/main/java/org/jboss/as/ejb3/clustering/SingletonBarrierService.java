/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.clustering;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

/**
 * A service installed as a sinleton, it is UP only on the master node of a the singleton barrier.
 *
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public class SingletonBarrierService implements Service<String> {
    public static final ServiceName SERVICE_NAME = ServiceName.parse("ejb3.cluster.barrier.singleton");

    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.info("this node is now considered the active cluster singleton");
    }

    public void stop(StopContext context) {
        ROOT_LOGGER.info("this node is no longer considered the active cluster singleton");
    }

    @Override
    public String getValue() {
        return "singleton barrier";
    }
}
