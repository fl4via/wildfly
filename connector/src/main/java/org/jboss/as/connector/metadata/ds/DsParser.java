/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.metadata.ds;

import org.jboss.as.connector.metadata.api.ds.DsSecurity;

import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.metadata.ParserException;
import org.jboss.jca.common.api.validator.ValidateException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * DsParser with Elytron support.
 * // FIXME dunno this is needed, I can't see DsParser being used in Wildfly
 *
 * @author Flavia Rainone
 */
public class DsParser extends org.jboss.jca.common.metadata.ds.DsParser {

    /**
     * Parse security
     * @param reader The reader
     * @return The result
     * @exception XMLStreamException XMLStreamException
     * @exception ParserException ParserException
     * @exception ValidateException ValidateException
     */
    protected DsSecurity parseDsSecurity(XMLStreamReader reader) throws XMLStreamException, ParserException,
            ValidateException {

        String userName = null;
        String password = null;
        String securityDomain = null;
        String elytronSecurityDomain = null;
        org.jboss.jca.common.api.metadata.common.Extension reauthPlugin = null;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT : {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY)
                    {

                        return new DsSecurityImpl(userName, password,
                                elytronSecurityDomain == null ? securityDomain : elytronSecurityDomain,
                                elytronSecurityDomain != null, reauthPlugin);
                    }
                    else
                    {
                        if (DsSecurity.Tag.forName(reader.getLocalName()) == DsSecurity.Tag.UNKNOWN)
                        {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT : {
                    DsSecurity.Tag tag = DsSecurity.Tag.forName(reader.getLocalName());
                    switch (tag) {
                        case PASSWORD : {
                            password = elementAsString(reader);
                            break;
                        }
                        case USER_NAME : {
                            userName = elementAsString(reader);
                            break;
                        }
                        case SECURITY_DOMAIN : {
                            securityDomain = elementAsString(reader);
                            break;
                        }
                        case ELYTRON_SECURITY_DOMAIN : {
                            elytronSecurityDomain = elementAsString(reader); // TODO validate
                            break;
                        }
                        case REAUTH_PLUGIN : {
                            reauthPlugin = parseExtension(reader, tag.getLocalName());
                            break;
                        }
                        default :
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

}
