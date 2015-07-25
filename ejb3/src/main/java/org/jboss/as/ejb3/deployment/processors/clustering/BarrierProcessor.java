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

package org.jboss.as.ejb3.deployment.processors.clustering;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ejb3.clustering.EJBBoundClusteringMetaData;
import org.jboss.as.ejb3.clustering.MdbBarrierService;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponent;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;

import java.util.ArrayList;
import java.util.List;

/**
 * Barrier DUP, disables automatic delivery of messages to MDBs associated with a barrier, and create an
 * MDBBarrierService to enable/disable delivery according to that MDBs barrier configuration..
 *
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public class BarrierProcessor implements DeploymentUnitProcessor {

    private static final Logger log = Logger.getLogger(BarrierProcessor.class);

    /**
     * See {@link Phase} for a description of the different phases
     */
    public static final Phase PHASE = Phase.INSTALL;

    /**
     * The relative order of this processor within the {@link #PHASE}.
     *
     * It needs to be just after INSTALL_EE_MODULE_CONFIG so the MDB components
     * can be modified during deployment
     */
    public static final int PRIORITY = Phase.INSTALL_EE_MODULE_CONFIG + 1;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleConfiguration moduleConfiguration = deploymentUnit.getAttachment(
                org.jboss.as.ee.component.Attachments.EE_MODULE_CONFIGURATION);
        if (moduleConfiguration == null) {
            return;
        }

        final List<EJBBoundClusteringMetaData> clusteringMetaData = getEJBBoundClusteringMetaData(phaseContext.getDeploymentUnit());
        if (clusteringMetaData == null || clusteringMetaData.isEmpty()) {
            return;
        }
        log.debug("handling EE deployment " + deploymentUnit.getName());
        for (final ComponentConfiguration configuration : moduleConfiguration.getComponentConfigurations()) {
            ComponentDescription description = configuration.getComponentDescription();
            if (description instanceof MessageDrivenComponentDescription) {
                final MessageDrivenComponentDescription mdbDescription = (MessageDrivenComponentDescription) description;
                final List<ServiceName> barrierDependencies = getBarrierDependencies(clusteringMetaData, mdbDescription);
                if (!barrierDependencies.isEmpty()) {
                    //set deliveryActive to false for this case as it will be
                    //controlled via the dependencies
                    mdbDescription.setDeliveryActive(false);

                    final ServiceName serviceName = description.getStartServiceName();
                    // install the HA MDB delivery service for the MDB
                    // these are passive, so they get started when they can, but
                    // do not report errors when not started
                    final MdbBarrierService mdbBarrierService = new MdbBarrierService();
                    final ServiceName mdbBarrierServiceName = createMdbBarrierServiceName(serviceName);
                    ServiceBuilder<MdbBarrierService> builder = phaseContext.getServiceTarget()
                            .addService(mdbBarrierServiceName, mdbBarrierService)
                            .addDependency(description.getCreateServiceName(), MessageDrivenComponent.class,
                                    mdbBarrierService.getMdbComponent()).setInitialMode(Mode.PASSIVE);
                    builder.addDependencies(barrierDependencies);

                    builder.install();
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        // dont even bother removing services if we have no clustering meta data
        final List<EJBBoundClusteringMetaData> clusteringMetaData = getEJBBoundClusteringMetaData(deploymentUnit);
        if (clusteringMetaData == null || clusteringMetaData.isEmpty()) {
            return;
        }
        final EEModuleConfiguration moduleConfiguration = deploymentUnit.getAttachment(
                org.jboss.as.ee.component.Attachments.EE_MODULE_CONFIGURATION);
        if (moduleConfiguration == null) {
            return;
        }
        // instead of recompiling the barrier dependencies for each mdb, it is quicker to just check if there is such a
        for (final ComponentConfiguration configuration : moduleConfiguration.getComponentConfigurations()) {
            ComponentDescription description = configuration.getComponentDescription();
            if (description instanceof MessageDrivenComponentDescription) {
                final ServiceName mdbBarrierServiceName = createMdbBarrierServiceName(description.getStartServiceName());
                final ServiceController<?> mdbBarrierService = deploymentUnit.getServiceRegistry().getService(
                        mdbBarrierServiceName);
                if (mdbBarrierService != null) {
                    mdbBarrierService.setMode(Mode.REMOVE);
                }
            }
        }
    }

    private List<EJBBoundClusteringMetaData> getEJBBoundClusteringMetaData(DeploymentUnit deploymentUnit) {
        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData == null) {
            return null;
        }
        final AssemblyDescriptorMetaData assemblyDescriptorMetaData = ejbJarMetaData.getAssemblyDescriptor();
        if (assemblyDescriptorMetaData == null) {
            return null;
        }
        // get the list of clustering meta data
        return assemblyDescriptorMetaData.getAny(EJBBoundClusteringMetaData.class);
    }

    private ServiceName createMdbBarrierServiceName(ServiceName mdbComponentName) {
        return mdbComponentName.append("MDB_DELIVERY");
    }

    private List<ServiceName> getBarrierDependencies(final List<EJBBoundClusteringMetaData> clusteringMetaData, final MessageDrivenComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        final List<ServiceName> result = new ArrayList<>();
        final String ejbName = componentConfiguration.getEJBName();
        for (final EJBBoundClusteringMetaData clusteringMD: clusteringMetaData) {
            if (clusteringMD.hasBarrier()) {
                final String clusteringEjbName = clusteringMD.getEjbName();
                if (clusteringEjbName.equals("*") || clusteringEjbName.equals(ejbName)) {
                    for (String barrier : clusteringMD.getBarriers()) {
                        log.debug("Found a deployment descriptor barrier dependency for [" + ejbName + "] *:" + barrier);
                        result.add(ServiceName.parse(barrier));
                    }
                }
            }
        }
        return result;
    }
}
