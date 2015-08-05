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
import org.jboss.msc.service.DelegatingServiceContainer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.singleton.SingletonPolicy;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

/**
 * Adds BarrierProcessor and the singleton barrier service.
 *
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public class ClusterBarrierAdd extends AbstractBoottimeAddStepHandler {

    static final ClusterBarrierAdd INSTANCE = new ClusterBarrierAdd();

    private ClusterBarrierAdd() {
        super(ClusterBarrierResourceDefinition.FULFILLS);
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        context.registerCapability(ClusterBarrierResourceDefinition.BASIC_CAPABILITY, null);
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

        try {
            final ServiceTarget serviceTarget = context.getServiceTarget();
            final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
            // install the HA singleton service
            final SingletonBarrierService service = new SingletonBarrierService();
            final SingletonPolicy singletonPolicy = (SingletonPolicy) serviceRegistry.getRequiredService(
                    context.getCapabilityServiceName(SingletonPolicy.CAPABILITY_NAME, SingletonPolicy.class)).getValue();
            //ServiceController<?> factoryService = serviceRegistry.getRequiredService(
            //        SingletonServiceName.BUILDER.getServiceName("server"));
            //SingletonServiceBuilderFactory factory = (SingletonServiceBuilderFactory) factoryService.getValue();
            //factory.createSingletonServiceBuilder(SERVICE_NAME, service)
            singletonPolicy.createSingletonServiceBuilder(ClusterBarrierResourceDefinition.BASIC_CAPABILITY.getCapabilityServiceName(), service)// "server", "default")
                    .build(new DelegatingServiceContainer(serviceTarget, serviceRegistry))
                    //.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, env)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();

            final String fulfills = ClusterBarrierResourceDefinition.FULFILLS.resolveModelAttribute(context, model).asString();
            if (fulfills != null) {
                serviceTarget.addService(ClusterBarrierResourceDefinition.getBarrierRequirementServiceName(fulfills), Service.NULL).install();
            }

        } catch (IllegalArgumentException e) {
            throw new OperationFailedException(e.getLocalizedMessage());
        }
    }

    // FIXME delete this or use it?
    private static class BarrierInstaller implements Service<Void> {

        private SingletonPolicy singletonPolicy;
        private final String fulfills;

        public BarrierInstaller(String fulfills) {
            this.fulfills = fulfills;
        }

        public void setSingletonPolicy(SingletonPolicy singletonPolicy) {
            this.singletonPolicy = singletonPolicy;
        }

        @Override public void start(StartContext context) throws StartException {
            final ServiceTarget target = context.getChildTarget();
            SingletonBarrierService service = new SingletonBarrierService();
                    singletonPolicy.createSingletonServiceBuilder(ClusterBarrierResourceDefinition.BASIC_CAPABILITY.getCapabilityServiceName(), service).build(target)
                            //.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, env)
                            .setInitialMode(ServiceController.Mode.ACTIVE).install();
            if (fulfills != null) {
                target.addService(ClusterBarrierResourceDefinition.getBarrierRequirementServiceName(fulfills), Service.NULL).install();
            }
        }

        @Override public void stop(StopContext context) {

        }

        @Override public Void getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }
    }

}
