/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Handler to remove clustering singleton barrier definition and corresponding service.
 *
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */ // TODO shouldn' t remove the DUP?
public class ClusterBarrierRemove extends ServiceRemoveStepHandler {

    public static final ClusterBarrierRemove INSTANCE = new ClusterBarrierRemove();

    private ClusterBarrierRemove() {
        super(ClusterBarrierResourceDefinition.BASIC_CAPABILITY.getCapabilityServiceName(), ClusterBarrierAdd.INSTANCE, ClusterBarrierResourceDefinition.BASIC_CAPABILITY);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) /* throws OperationFailedException*/ {
        final String fulfills;
        try {
            fulfills = ClusterBarrierResourceDefinition.FULFILLS.resolveModelAttribute(context, model).asString();
        } catch (OperationFailedException e) {
            // FIXME shouldn' t super class throw OperationFailedException
            throw new RuntimeException(e);
        }
        if (fulfills != null) {
            context.removeService(ClusterBarrierResourceDefinition.getBarrierRequirementServiceName(fulfills));
        }
        super.performRuntime(context, operation, model);
    }

    @Override
    protected void recoverServices(final OperationContext context, final ModelNode operation, final ModelNode model)
            throws OperationFailedException {
        try {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            ClusterBarrierAdd.INSTANCE.installServices(context, model);
        } catch (OperationFailedException e) {
            throw ControllerLogger.ROOT_LOGGER.failedToRecoverServices(e);
        }
    }

}
