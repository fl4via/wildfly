/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.connector.metadata.ironjacamar;

import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.as.connector.metadata.api.common.Security;
import org.jboss.as.connector.metadata.api.resourceadapter.WorkManagerSecurity;
import org.jboss.as.connector.metadata.common.CredentialImpl;
import org.jboss.as.connector.metadata.common.SecurityImpl;
import org.jboss.as.connector.metadata.resourceadapter.WorkManagerSecurityImpl;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.resourceadapter.WorkManager;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.ParserException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extension of {@link org.jboss.jca.common.metadata.ironjacamar.IronJacamarParser} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public class IronJacamarParser extends org.jboss.jca.common.metadata.ironjacamar.IronJacamarParser {
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
        String securityDomainAndApplication = null;
        String authenticationContext = null;
        String authenticationContextAndApplication = null;
        boolean elytronEnabled = false;

        boolean application = org.jboss.jca.common.api.metadata.Defaults.APPLICATION_MANAGED_SECURITY;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT : {
                    if (org.jboss.jca.common.api.metadata.ds.DataSource.Tag.forName(reader.getLocalName()) == org.jboss.jca.common.api.metadata.ds.DataSource.Tag.SECURITY)
                    {

                        return new SecurityImpl(elytronEnabled? authenticationContext : securityDomain,
                                elytronEnabled? authenticationContextAndApplication : securityDomainAndApplication,
                                application, elytronEnabled);
                    }
                    else
                    {
                        if (Security.Tag.forName(reader.getLocalName()) == Security.Tag.UNKNOWN)
                        {
                            throw new ParserException("unexpectedEndTag(reader.getLocalName())"); // FIXME should I use ExtendedStreamReader?
                        }
                    }
                    break;
                }
                case START_ELEMENT : {
                    switch (Security.Tag.forName(reader.getLocalName())) {

                        case SECURITY_DOMAIN : {
                            securityDomain = elementAsString(reader);
                            break;
                        }
                        case SECURITY_DOMAIN_AND_APPLICATION : {
                            securityDomain = elementAsString(reader);
                            break;
                        }
                        case ELYTRON_ENABLED: {
                            elytronEnabled = true;
                        }
                        case AUTHENTICATION_CONTEXT: {
                            authenticationContext = elementAsString(reader);
                            break;
                        }
                        case AUTHENTICATION_CONTEXT_AND_APPLICATION: {
                            authenticationContextAndApplication = elementAsString(reader);
                            break;
                        }
                        case APPLICATION : {
                            application = elementAsBoolean(reader);
                            break;
                        }
                        default :
                            throw new ParserException("bundle.unexpectedElement(reader.getLocalName())"); // FIXME same thing here
                    }
                    break;
                }
            }
        }
        throw new ParserException("bundle.unexpectedEndOfDocument()"); // FIXME same thing
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
        boolean elytronEnabled = false;
        String authenticationContext = null;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT : {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY ||
                            Recovery.Tag.forName(reader.getLocalName()) == Recovery.Tag.RECOVER_CREDENTIAL) {

                        return new CredentialImpl(userName, password,
                                elytronEnabled? authenticationContext : securityDomain, elytronEnabled, null);
                    } else if (Credential.Tag.forName(reader.getLocalName()) == Credential.Tag.UNKNOWN) {
                        throw new ParserException("bundle.unexpectedEndTag(reader.getLocalName())");
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
                        case ELYTRON_ENABLED : {
                            elytronEnabled = true;
                            break;
                        }
                        case AUTHENTICATION_CONTEXT: {
                            authenticationContext = elementAsString(reader);
                            break;
                        }
                        default :
                            throw new ParserException("bundle.unexpectedElement(reader.getLocalName())");
                    }
                    break;
                }
            }
        }
        throw new ParserException("bundle.unexpectedEndOfDocument()");
    }

    /**
     * Parse workmanager's security element
     * @param reader The reader
     * @return The value
     * @exception XMLStreamException XMLStreamException
     * @exception ParserException ParserException
     * @exception ValidateException ValidateException
     */
    protected WorkManagerSecurity parseWorkManagerSecurity(XMLStreamReader reader) throws XMLStreamException,
            ParserException, ValidateException {
        boolean mappingRequired = false;
        String domain = null;
        boolean elytronEnabled = false;
        String defaultPrincipal = null;
        List<String> defaultGroups = null;
        Map<String, String> userMappings = null;
        Map<String, String> groupMappings = null;

        boolean userMappingEnabled = false;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT : {
                    if (WorkManager.Tag.forName(reader.getLocalName()) == WorkManager.Tag.SECURITY) {
                        return new WorkManagerSecurityImpl(mappingRequired, domain, elytronEnabled,
                                defaultPrincipal, defaultGroups,
                                userMappings, groupMappings);
                    } else {
                        if (WorkManagerSecurity.Tag.forName(reader.getLocalName()) == WorkManagerSecurity.Tag.UNKNOWN) {
                            throw new ParserException("bundle.unexpectedEndTag(reader.getLocalName())");
                        }
                    }
                    break;
                }
                case START_ELEMENT : {
                    switch (WorkManagerSecurity.Tag.forName(reader.getLocalName())) {
                        case DEFAULT_GROUPS :
                        case MAPPINGS : {
                            // Skip
                            break;
                        }
                        case MAPPING_REQUIRED : {
                            mappingRequired = elementAsBoolean(reader);
                            break;
                        }
                        case DOMAIN : {
                            domain = elementAsString(reader);
                            break;
                        }
                        case ELYTRON_SECURITY_DOMAIN: {
                            domain = elementAsString(reader);
                            elytronEnabled = false;
                            break;
                        }
                        case DEFAULT_PRINCIPAL : {
                            defaultPrincipal = elementAsString(reader);
                            break;
                        }
                        case GROUP : {
                            if (defaultGroups == null)
                                defaultGroups = new ArrayList<String>(1);

                            defaultGroups.add(elementAsString(reader));
                            break;
                        }
                        case USERS : {
                            userMappingEnabled = true;
                            break;
                        }
                        case GROUPS : {
                            userMappingEnabled = false;
                            break;
                        }
                        case MAP : {
                            if (userMappingEnabled) {
                                if (userMappings == null)
                                    userMappings = new HashMap<String, String>();

                                String from = attributeAsString(reader, WorkManagerSecurity.Attribute.FROM.getLocalName());

                                if (from == null || from.trim().equals(""))
                                    throw new ParserException(
                                            "bundle.requiredAttributeMissing(WorkManagerSecurity.Attribute.FROM.getLocalName(), reader.getLocalName())");

                                String to = attributeAsString(reader, WorkManagerSecurity.Attribute.TO.getLocalName());

                                if (to == null || to.trim().equals(""))
                                    throw new ParserException(
                                            "bundle.requiredAttributeMissing(WorkManagerSecurity.Attribute.TO.getLocalName()," +
                                                    "reader.getLocalName())");

                                userMappings.put(from, to);
                            } else {
                                if (groupMappings == null)
                                    groupMappings = new HashMap<String, String>();

                                String from = attributeAsString(reader, WorkManagerSecurity.Attribute.FROM.getLocalName());

                                if (from == null || from.trim().equals(""))
                                    throw new ParserException(
                                            "bundle.requiredAttributeMissing(WorkManagerSecurity.Attribute.FROM.getLocalName(),"
                                                    + "reader.getLocalName())");

                                String to = attributeAsString(reader, WorkManagerSecurity.Attribute.TO.getLocalName());

                                if (to == null || to.trim().equals(""))
                                    throw new ParserException(
                                            "bundle.requiredAttributeMissing(WorkManagerSecurity.Attribute.TO.getLocalName()," +
                                                    "reader.getLocalName())");

                                groupMappings.put(from, to);
                            }
                            break;
                        }
                        default :
                            throw new ParserException("bundle.unexpectedElement(reader.getLocalName())");
                    }
                    break;
                }
            }
        }
        throw new ParserException("bundle.unexpectedEndOfDocument()");
    }
}
