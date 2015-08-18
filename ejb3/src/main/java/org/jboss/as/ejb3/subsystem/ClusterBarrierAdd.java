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
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.singleton.SingletonPolicy;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.subsystem.ClusterBarrierResourceDefinition.BARRIER_CAPABILITY;

/**
 * Adds the singleton barrier service.
 *
 * @author Flavia Rainone
 */
public class ClusterBarrierAdd extends AbstractBoottimeAddStepHandler {

    static final ClusterBarrierAdd INSTANCE = new ClusterBarrierAdd();

    private ClusterBarrierAdd() {
        super();
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
        installServices(context);
    }

    protected void installServices(final OperationContext context) throws OperationFailedException {
         final ServiceTarget serviceTarget = context.getServiceTarget();

         // install the parent service
         final ClusterBarrierCreator serviceCreator = new ClusterBarrierCreator();
         serviceTarget.addService(BARRIER_CAPABILITY.getCapabilityServiceName("creator"), serviceCreator)
                 .addDependency(context.getCapabilityServiceName(SingletonPolicy.CAPABILITY_NAME, SingletonPolicy.class),
                         SingletonPolicy.class, serviceCreator.getSingletonPolicy())
                 .install();
    }

    private static class ClusterBarrierCreator extends AbstractService<Void> {

        private final InjectedValue<SingletonPolicy> singletonPolicy;

        public ClusterBarrierCreator() {
            this.singletonPolicy = new InjectedValue<>();
        }

        public InjectedValue<SingletonPolicy> getSingletonPolicy() {
            return singletonPolicy;
        }

        @Override public void start(StartContext context) throws StartException {
            final ServiceTarget target = context.getChildTarget();
            final SingletonPolicy singletonPolicyValue = singletonPolicy.getValue();
            final SingletonBarrierService service = new SingletonBarrierService();
            singletonPolicyValue.createSingletonServiceBuilder(BARRIER_CAPABILITY.getCapabilityServiceName(), service)
                    .build(target)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
        }
    }
}
