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

package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.auth.server.SecurityDomain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static org.jboss.as.connector.subsystems.jca.Constants.JCA_SECURITY_DOMAIN;

/**
 * A {@link ResourceDefinition} to define the mapping from a security domain as specified in a rar or ds to an Elytron
 * security domain.
 *
 * @author Flavia Rainone
 */
public class JcaSecurityDomainDefinition extends PersistentResourceDefinition {

    protected static final PathElement PATH_JCA_SECURITY_DOMAIN = PathElement.pathElement(JCA_SECURITY_DOMAIN);

    public static final String JCA_SECURITY_DOMAIN_CAPABILITY = PATH_JCA_SECURITY_DOMAIN.getValue(); //"org.wildfly.extension.undertow.application-security-domain";

    static final RuntimeCapability<Void> JCA_SECURITY_DOMAIN_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(JCA_SECURITY_DOMAIN_CAPABILITY, true, Function.class)
            .build();

    static final SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(Constants.SECURITY_DOMAIN, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setCapabilityReference(JCA_SECURITY_DOMAIN_CAPABILITY, JCA_SECURITY_DOMAIN_CAPABILITY, true)
            .build();

    private static StringListAttributeDefinition REFERENCING_DEPLOYMENTS = new StringListAttributeDefinition.Builder(Constants.REFERENCING_DEPLOYMENTS)
            .setStorageRuntime()
            .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { SECURITY_DOMAIN };



    static final JcaSecurityDomainDefinition INSTANCE = new JcaSecurityDomainDefinition();

    private static final Set<String> knownApplicationSecurityDomains = Collections.synchronizedSet(new HashSet<>());

    private JcaSecurityDomainDefinition() {
        this(new Parameters(PATH_JCA_SECURITY_DOMAIN, JcaExtension.getResourceDescriptionResolver(PATH_JCA_SECURITY_DOMAIN.getKey()))
                .setCapabilities(JCA_SECURITY_DOMAIN_RUNTIME_CAPABILITY), new AddHandler());
    }

    private JcaSecurityDomainDefinition(Parameters parameters, AbstractAddStepHandler add) {
        super(parameters.setAddHandler(add).setRemoveHandler(new RemoveHandler(add)));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        knownApplicationSecurityDomains.clear(); // If we are registering, time for a clean start.
        super.registerAttributes(resourceRegistration);
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute: ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute,  null, handler);
        }
        resourceRegistration.registerReadOnlyAttribute(REFERENCING_DEPLOYMENTS, new ReferencingDeploymentsHandler());
    }

    private static class AddHandler extends AbstractAddStepHandler {

        private AddHandler() {
            super(ATTRIBUTES);
        }

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.populateModel(context, operation, resource);
            knownApplicationSecurityDomains.add(context.getCurrentAddressValue());
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String securityDomain = SECURITY_DOMAIN.resolveModelAttribute(context, model).asString();

            RuntimeCapability<?> runtimeCapability = JCA_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(context.getCurrentAddressValue());
            ServiceName serviceName = runtimeCapability.getCapabilityServiceName( JcaSecurityDomainService.JcaSecurityDomain.class);
            JcaSecurityDomainService applicationSecurityDomainService = new JcaSecurityDomainService();

            ServiceBuilder<JcaSecurityDomainService.JcaSecurityDomain> serviceBuilder = context.getServiceTarget().addService(serviceName, applicationSecurityDomainService)
                    .setInitialMode(Mode.LAZY);
            serviceBuilder.addDependency(context.getCapabilityServiceName(
                    JCA_SECURITY_DOMAIN_CAPABILITY, securityDomain, SecurityDomain.class),
                    SecurityDomain.class, applicationSecurityDomainService.getSecurityDomainInjector());
            serviceBuilder.install();
        }
    }

    private static class RemoveHandler extends ServiceRemoveStepHandler {

        protected RemoveHandler(AbstractAddStepHandler addOperation) {
            super(addOperation);
        }

        @Override
        protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRemove(context, operation, model);
            knownApplicationSecurityDomains.remove(context.getCurrentAddressValue());
        }

        @Override
        protected ServiceName serviceName(String name) {
            RuntimeCapability<?> dynamicCapability = JCA_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(name);
            return dynamicCapability.getCapabilityServiceName(JcaSecurityDomainService.JcaSecurityDomain.class);
        }
    }
    private static class ReferencingDeploymentsHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            RuntimeCapability<Void> runtimeCapability = JCA_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(context.getCurrentAddressValue());
            ServiceName jcaSecurityDomainName = runtimeCapability.getCapabilityServiceName(Function.class);

            ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
            ServiceController<?> controller = serviceRegistry.getRequiredService(jcaSecurityDomainName);

            ModelNode deploymentList = new ModelNode();
            if (controller.getState() == State.UP) {
                Service service = controller.getService();
                if (service instanceof JcaSecurityDomainService) {
                    for (String current : ((JcaSecurityDomainService)service).getDeployments()) {
                        deploymentList.add(current);
                    }
                }
            }

            context.getResult().set(deploymentList);
        }

    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

}
