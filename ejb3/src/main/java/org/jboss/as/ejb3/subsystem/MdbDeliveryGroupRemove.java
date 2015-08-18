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

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.dmr.ModelNode;

/**
 * Handler to remove a mdb delivery group.
 *
 * @author Flavia Rainone
 */
public class MdbDeliveryGroupRemove extends AbstractRemoveStepHandler {

    public static final MdbDeliveryGroupRemove INSTANCE = new MdbDeliveryGroupRemove();

    private MdbDeliveryGroupRemove() {
        super();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        context.reloadRequired();
    }

    @Override
    protected void recoverServices(final OperationContext context, final ModelNode operation, final ModelNode model)
            throws OperationFailedException {
        try {
            MdbDeliveryGroupAdd.INSTANCE.installServices(context, operation, model);
        } catch (OperationFailedException e) {
            throw ControllerLogger.ROOT_LOGGER.failedToRecoverServices(e);
        }
    }
}
