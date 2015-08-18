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

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ejb3.clustering.SingletonBarrierService;
import org.wildfly.clustering.singleton.SingletonPolicy;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for clustering singleton barrier.
 *
 * @author Flavia Rainone
 */
public class ClusterBarrierResourceDefinition extends SimpleResourceDefinition {

    public static final RuntimeCapability<Void> BARRIER_CAPABILITY =  RuntimeCapability.Builder.of(
            "org.wildfly.ejb3.cluster.barrier", SingletonBarrierService.class)
            .addRequirements(SingletonPolicy.CAPABILITY_NAME.concat(".default")).build();

    public static final ClusterBarrierResourceDefinition INSTANCE = new ClusterBarrierResourceDefinition();

    private ClusterBarrierResourceDefinition() {
        super(EJB3SubsystemModel.CLUSTER_BARRIER_PATH,
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.CLUSTER_BARRIER), ClusterBarrierAdd.INSTANCE,
                ClusterBarrierRemove.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(ClusterBarrierElect.OPERATION_DEFINITION, ClusterBarrierElect.INSTANCE,
                false);
    }
}
