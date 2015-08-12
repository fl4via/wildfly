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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ejb3.clustering.SingletonBarrierService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.Service;
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

    public static final SimpleAttributeDefinition FULFILLS =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CLUSTER_BARRIER_FULFILLS, ModelType.STRING, true)
                    .setValidator(new ModelTypeValidator(ModelType.STRING, true, false))
                    .build();

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

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(FULFILLS, null,
                new AbstractWriteAttributeHandler<Void>() {
                    @Override protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation,
                            String attributeName, ModelNode resolvedValue, ModelNode currentValue,
                            HandbackHolder<Void> handbackHolder) throws OperationFailedException {
                        replaceBarrierFulfillment(context, currentValue, resolvedValue);
                        return false;
                    }

                    @Override protected void revertUpdateToRuntime(OperationContext context, ModelNode operation,
                            String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback)
                            throws OperationFailedException {
                        replaceBarrierFulfillment(context, valueToRevert, valueToRestore);
                    }

                    protected void replaceBarrierFulfillment(OperationContext context, ModelNode serviceToRemove, ModelNode serviceToAdd) throws OperationFailedException {
                        context.removeService(BARRIER_CAPABILITY.getCapabilityServiceName(serviceToRemove.asString()));
                        context.getServiceTarget().addService(BARRIER_CAPABILITY.getCapabilityServiceName(serviceToAdd.asString()), Service.NULL).install();
                    }
        });
    }
}
