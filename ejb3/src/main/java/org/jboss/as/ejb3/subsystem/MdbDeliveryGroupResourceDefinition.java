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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.msc.service.ServiceName;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for mdb delivery group.
 *
 * @author Flavia Rainone
 */
public class MdbDeliveryGroupResourceDefinition extends SimpleResourceDefinition {

    private static final ServiceName DELIVERY_GROUP_SERVICE_NAME = ServiceName.of("org", "wildfly", "ejb3", "mdb", "delivery", "group");

    public static final MdbDeliveryGroupResourceDefinition INSTANCE = new MdbDeliveryGroupResourceDefinition();

    private MdbDeliveryGroupResourceDefinition() {
        super(PathElement.pathElement(EJB3SubsystemModel.MDB_DELIVERY_GROUP),
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.MDB_DELIVERY_GROUP), MdbDeliveryGroupAdd.INSTANCE,
                MdbDeliveryGroupRemove.INSTANCE);
    }

    public static final ServiceName getDeliveryGroupServiceName(String deliveryGroupName) {
        return DELIVERY_GROUP_SERVICE_NAME.append(deliveryGroupName);
    }

}
