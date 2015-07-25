/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.clustering;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Parses the urn:clustering namespace elements for clustering related metadata on EJBs.
 *
 * @author Jaikiran Pai
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public class EJBBoundClusteringMetaDataParser extends AbstractEJBBoundMetaDataParser<EJBBoundClusteringMetaData> {

    public static final String NAMESPACE_URI = "urn:clustering:1.0";
    private static final String ROOT_ELEMENT_CLUSTERING = "clustering";
    private static final String BARRIER_ELEMENT =  "barrier";
    private static final String DEPENDS_ELEMENT =  "depends";

    @Override
    public EJBBoundClusteringMetaData parse(final XMLStreamReader xmlStreamReader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        final String element = xmlStreamReader.getLocalName();
        // we only parse <clustering> (root) element
        if (!ROOT_ELEMENT_CLUSTERING.equals(element)) {
            throw unexpectedElement(xmlStreamReader);
        }
        EJBBoundClusteringMetaData metaData = new EJBBoundClusteringMetaData();
        this.processElements(metaData, xmlStreamReader, propertyReplacer);
        return metaData;
    }

    @Override
    protected void processElement(final EJBBoundClusteringMetaData metaData, final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        if (NAMESPACE_URI.equals(reader.getNamespaceURI())) {
            EjbLogger.ROOT_LOGGER.deprecatedNamespace(NAMESPACE_URI, ROOT_ELEMENT_CLUSTERING);
            final String localName = reader.getLocalName();
            if (localName.equals("clustered")) {
                getElementText(reader, propertyReplacer);
            } else if (localName.equals(BARRIER_ELEMENT)){
                if (reader.hasNext()) {
                    if (reader.nextTag() == END_ELEMENT) {
                        metaData.addBarrier(SingletonBarrierService.SERVICE_NAME.append("service" ).getCanonicalName());
                    } else do {
                            if (reader.getLocalName().equals(DEPENDS_ELEMENT)){
                                // TODO check if element text is not empty
                                metaData.addBarrier(getElementText(reader, propertyReplacer));
                                if (reader.nextTag() != END_ELEMENT) {
                                    throw unexpectedElement(reader);
                                }
                            } else {
                                throw unexpectedElement(reader);
                            }
                        } while (reader.hasNext() && reader.nextTag() != END_ELEMENT);
                    }
                //reader.nextTag();// read END_ELEMENT
            } else {
                throw unexpectedElement(reader);
            }
        } else {
            super.processElement(metaData, reader, propertyReplacer);
        }
    }
}
