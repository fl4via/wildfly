/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;
import java.util.List;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;

/**
 * Parser for ejb3:3.1 namespace.
 *
 * @author Flavia Rainone
 */
public class EJB3Subsystem31Parser extends EJB3Subsystem30Parser {

    public static final EJB3Subsystem31Parser INSTANCE = new EJB3Subsystem31Parser();

    protected EJB3Subsystem31Parser() {
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_3_1;
    }

    @Override
    protected void readElement(final XMLExtendedStreamReader reader, final EJB3SubsystemXMLElement element, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        switch (element) {
            case CLUSTER_BARRIER: {
                parseClusterBarrier(reader, operations);
                break;
            }
            default: {
                super.readElement(reader, element, operations, ejb3SubsystemAddOperation);
            }
        }
    }

    protected void parseClusterBarrier(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        ModelNode addOperation = Util.createAddOperation(SUBSYSTEM_PATH.append(EJB3SubsystemModel.CLUSTER_BARRIER_PATH));
        requireNoAttributes(reader);
        requireNoContent(reader);
        operations.add(addOperation);
    }
}
