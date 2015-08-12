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

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.clustering.SingletonBarrierService;
import org.jboss.as.ejb3.deployment.processors.clustering.BarrierProcessor;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.singleton.SingletonPolicy;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.subsystem.ClusterBarrierResourceDefinition.BARRIER_CAPABILITY;

/**
 * Adds BarrierProcessor and the singleton barrier service.
 *
 * @author Flavia Rainone
 */
public class ClusterBarrierAdd extends AbstractBoottimeAddStepHandler {

    static final ClusterBarrierAdd INSTANCE = new ClusterBarrierAdd();

    private ClusterBarrierAdd() {
        super(ClusterBarrierResourceDefinition.FULFILLS);
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        context.registerCapability(BARRIER_CAPABILITY, null);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                ROOT_LOGGER.debug("Adding BarrierProcessor");
                processorTarget
                        .addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_EE_MODULE_CONFIG + 1,
                                new BarrierProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
        installServices(context, model);
    }

    protected void installServices(final OperationContext context, final ModelNode model) throws OperationFailedException {
         final ServiceTarget serviceTarget = context.getServiceTarget();
         final String fulfills = ClusterBarrierResourceDefinition.FULFILLS.resolveModelAttribute(context, model).asString();

         // install the parent service
         final ClusterBarrierParent parentService = new ClusterBarrierParent(fulfills);
         serviceTarget.addService(BARRIER_CAPABILITY.getCapabilityServiceName("parent"), parentService)
                 .addDependency(context.getCapabilityServiceName(SingletonPolicy.CAPABILITY_NAME, SingletonPolicy.class),
                         SingletonPolicy.class, parentService.getSingletonPolicy())
                 .install();
    }

    private static class ClusterBarrierParent extends AbstractService<Void> {

        private final String fulfills;
        private final InjectedValue<SingletonPolicy> singletonPolicy;

        public ClusterBarrierParent(String fulfills) {
            this.fulfills = fulfills;
            this.singletonPolicy = new InjectedValue<>();
        }

        public InjectedValue<SingletonPolicy> getSingletonPolicy() {
            return singletonPolicy;
        }

        @Override public void start(StartContext context) throws StartException {
            final ServiceTarget target = context.getChildTarget();
            SingletonPolicy singletonPolicyValue = singletonPolicy.getValue();
            SingletonBarrierService service = new SingletonBarrierService();
                    singletonPolicyValue.createSingletonServiceBuilder(BARRIER_CAPABILITY.getCapabilityServiceName(), service).build(target)
                            .setInitialMode(ServiceController.Mode.ACTIVE).install();
            if (fulfills != null) {
                target.addService(BARRIER_CAPABILITY.getCapabilityServiceName(fulfills), Service.NULL).install();
            }
        }
    }
}
