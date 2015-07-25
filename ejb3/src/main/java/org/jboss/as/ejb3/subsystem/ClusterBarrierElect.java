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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

// TODO
/**
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public class ClusterBarrierElect implements OperationStepHandler {
    public static final ClusterBarrierElect INSTANCE = new ClusterBarrierElect();
    public static final String OPERATION_NAME = "elect";

    static final SensitiveTargetAccessConstraintDefinition ELECT_CONSTRAINT = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(PathElement.pathElement(EJB3SubsystemModel.CLUSTER_BARRIER).toString(), OPERATION_NAME, false, true, true));

    static final SimpleOperationDefinition OPERATION_DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.CLUSTER_BARRIER))
            .addAccessConstraint(ELECT_CONSTRAINT)
            .withFlag(OperationEntry.Flag.RUNTIME_ONLY)
            .build();

    @SuppressWarnings("unchecked")
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final ModelNode resultNode = context.getResult();
        resultNode.set("Elect operation called");
        ROOT_LOGGER.info("Elect operation called");
        //ServiceController<SingletonService<String>> controller = (ServiceController<SingletonService<String>>) context.getServiceRegistry(false).getRequiredService(ClusterSingletonControllerService.SERVICE_NAME);
        //ServiceController<String> controller = (ServiceController<String>) context.getServiceRegistry(false).getRequiredService(
        //        SingletonBarrierService.SERVICE_NAME);
        //The actual singleton implementation is wrapped by an AsynchronousService, so a getValue() call is needed to get the underlying service.
        //AsynchronousService<String> asyncService = (AsynchronousService<String>) controller.getService();

        //SingletonService<String> singletonService = (SingletonService<String>) asyncService.getService();
        //singletonService.electMyself();
    }
}
