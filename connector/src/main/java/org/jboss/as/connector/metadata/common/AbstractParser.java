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

package org.jboss.as.connector.metadata.common;


import org.jboss.as.connector.metadata.api.Credential;
import org.jboss.as.connector.metadata.api.Security;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.metadata.ParserException;
import org.jboss.jca.common.api.validator.ValidateException;

import javax.xml.stream.XMLStreamException;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

/**
 * @author Flavia Rainone
 */
// TODO: we need to add this parser to the hierarchy, instead of copying the entire hierarchy, I plan to add some parsing
// TODO: api to the security integration API in ironjacamar, lets discuss this in the team call
public class AbstractParser extends org.jboss.jca.common.metadata.common.AbstractParser {

    /**
     *
     * parse a {@link Security} element
     *
     * @param reader reader
     * @return a {@link Security} object
     * @throws XMLStreamException XMLStreamException
     * @throws ParserException ParserException
     * @throws ValidateException ValidateException
     */
    protected Security parseSecuritySettings(javax.xml.stream.XMLStreamReader reader) throws XMLStreamException, ParserException,
            org.jboss.jca.common.api.validator.ValidateException {

        String securityDomain = null;
        String elytronSecurityDomain = null;
        String securityDomainAndApplication = null;
        String elytronSecurityDomainAndApplication = null;
        boolean application = org.jboss.jca.common.api.metadata.Defaults.APPLICATION_MANAGED_SECURITY;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT : {
                    if (org.jboss.jca.common.api.metadata.ds.DataSource.Tag.forName(reader.getLocalName()) == org.jboss.jca.common.api.metadata.ds.DataSource.Tag.SECURITY)
                    {

                        return new SecurityImpl(elytronSecurityDomain == null? securityDomain: elytronSecurityDomain,
                                elytronSecurityDomainAndApplication == null? securityDomainAndApplication: elytronSecurityDomainAndApplication,
                                application, elytronSecurityDomain != null || elytronSecurityDomainAndApplication != null);
                    }
                    else
                    {
                        if (Security.Tag.forName(reader.getLocalName()) == Security.Tag.UNKNOWN)
                        {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT : {
                    switch (Security.Tag.forName(reader.getLocalName())) {

                        case ELYTRON_SECURITY_DOMAIN : {
                            elytronSecurityDomain = elementAsString(reader);
                            if (securityDomain != null)
                            {
                                throw new ParserException(ROOT_LOGGER.duplicateSecurityDomain(elytronSecurityDomain,
                                        securityDomain));
                            }
                            break;
                        }
                        case SECURITY_DOMAIN : {
                            securityDomain = elementAsString(reader);
                            if (elytronSecurityDomain != null)
                            {
                                throw new ParserException(ROOT_LOGGER.duplicateSecurityDomain(elytronSecurityDomain,
                                        securityDomain));
                            }

                            break;
                        }
                        case ELYTRON_SECURITY_DOMAIN_AND_APPLICATION:
                            elytronSecurityDomainAndApplication = elementAsString(reader);
                            if (securityDomainAndApplication != null)
                            {
                                throw new ParserException(ROOT_LOGGER.duplicateSecurityDomainAndApplication(
                                        elytronSecurityDomainAndApplication, securityDomainAndApplication));
                            }
                            break;
                        case SECURITY_DOMAIN_AND_APPLICATION : {
                            securityDomainAndApplication = elementAsString(reader);
                            if (elytronSecurityDomainAndApplication != null)
                            {
                                throw new ParserException(ROOT_LOGGER.duplicateSecurityDomainAndApplication(
                                        elytronSecurityDomainAndApplication, securityDomainAndApplication));
                            }
                            break;
                        }
                        case APPLICATION : {
                            application = elementAsBoolean(reader);
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

    /**
     *
     * parse credential tag
     *
     * @param reader reader
     * @return the parse Object
     * @throws XMLStreamException in case of error
     * @throws ParserException in case of error
     * @throws ValidateException in case of error
     */
    protected Credential parseCredential(javax.xml.stream.XMLStreamReader reader) throws XMLStreamException, ParserException,
            ValidateException {

        String userName = null;
        String password = null;
        String securityDomain = null;
        String elytronSecurityDomain = null;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT : {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY ||
                            Recovery.Tag.forName(reader.getLocalName()) == Recovery.Tag.RECOVER_CREDENTIAL) {

                        return new CredentialImpl(userName, password,
                                elytronSecurityDomain == null ? securityDomain : elytronSecurityDomain,
                                elytronSecurityDomain != null);
                    } else if (Credential.Tag.forName(reader.getLocalName()) == Credential.Tag.UNKNOWN) {
                        throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                    }
                    break;
                }
                case START_ELEMENT : {
                    switch (Credential.Tag.forName(reader.getLocalName())) {
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
