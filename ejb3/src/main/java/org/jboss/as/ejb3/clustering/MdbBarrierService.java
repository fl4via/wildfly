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


import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponent;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;


/**
 * Service that represents a cluster barrier for a specific MDB.
 *
 * When started, delivery to a mdb is enabled, when stopped, it is disabled.
 *
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public class MdbBarrierService implements Service<MdbBarrierService> {
    private static final Logger log = Logger.getLogger(MdbBarrierService.class);

    private final InjectedValue<MessageDrivenComponent> mdbComponent = new InjectedValue<MessageDrivenComponent>();

    public MdbBarrierService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<MessageDrivenComponent> getMdbComponent() {
        return mdbComponent;
    }

    public void start(final StartContext context) throws StartException {
        MessageDrivenComponent mdb = mdbComponent.getValue();
        mdb.startDelivery();
        log.info("MDB delivery started: " + mdb.getApplicationName() + ", " + mdb.getComponentName());
    }

    public void stop(final StopContext context) {
        MessageDrivenComponent mdb = mdbComponent.getValue();
        mdb.stopDelivery();
        log.info("MDB delivery stopped: " + mdb.getApplicationName() + ", " + mdb.getComponentName());
    }
}
