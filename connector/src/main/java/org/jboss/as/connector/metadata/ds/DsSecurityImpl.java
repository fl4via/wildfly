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

import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.validator.ValidateException;

import org.jboss.as.connector.metadata.api.ds.DsSecurity;

/**
 * DsSecurityImpl metadata with Elytron support.
 *
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public class DsSecurityImpl
        extends org.jboss.jca.common.metadata.ds.DsSecurityImpl implements DsSecurity {

    /** The serialVersionUID */
    private static final long serialVersionUID = -5782260654400841898L;

    /**
     * Indicates if the Credential data belongs to Elytron or PicketBox.
     */
    private boolean elytronEnabled;

    /**
     * Create a new DsSecurityImpl.
     *
     * @param userName userName
     * @param password password
     * @param securityDomain securityDomain
     * @param elytronEnabled elytronEnabled
     * @param reauthPlugin reauthPlugin
     * @throws ValidateException in case of validation error
     */
    public DsSecurityImpl(String userName, String password, String securityDomain, boolean elytronEnabled,
            Extension reauthPlugin) throws ValidateException {
        super(userName, password, securityDomain, reauthPlugin);
        this.elytronEnabled = elytronEnabled;
    }

    /**
     * Does the security domain belongs to Elytron.
     *
     * @return {@code true} if is the domain elytron enabled
     */
    @Override
    public final boolean isElytronEnabled() {
        return elytronEnabled;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + (elytronEnabled? 1: 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof DsSecurityImpl)) {
            DsSecurityImpl other = (DsSecurityImpl) obj;
            return elytronEnabled == other.elytronEnabled && super.equals(other);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        if (!elytronEnabled)
            return super.toString();

        StringBuilder sb = new StringBuilder();

        sb.append("<security>");

        if (getUserName() != null) {
            sb.append("<").append(DsSecurity.Tag.USER_NAME).append(">");
            sb.append(getUserName());
            sb.append("</").append(DsSecurity.Tag.USER_NAME).append(">");

            sb.append("<").append(DsSecurity.Tag.PASSWORD).append(">");
            sb.append(getPassword());
            sb.append("</").append(DsSecurity.Tag.PASSWORD).append(">");
        }
        else if (getSecurityDomain() != null) {
            sb.append("<").append(DsSecurity.Tag.ELYTRON_SECURITY_DOMAIN).append(">");
            sb.append(getSecurityDomain());
            sb.append("</").append(DsSecurity.Tag.ELYTRON_SECURITY_DOMAIN).append(">");
        }

        if (getReauthPlugin() != null) {
            sb.append("<").append(DsSecurity.Tag.REAUTH_PLUGIN);
            sb.append(" ").append(Extension.Attribute.CLASS_NAME).append("=\"");
            sb.append(getReauthPlugin().getClassName()).append("\"");
            sb.append(">");

            if (getReauthPlugin().getConfigPropertiesMap().size() > 0) {
                java.util.Iterator<java.util.Map.Entry<String, String>> it = getReauthPlugin().getConfigPropertiesMap().entrySet().iterator();

                while (it.hasNext()) {
                    java.util.Map.Entry<String, String> entry = it.next();

                    sb.append("<").append(Extension.Tag.CONFIG_PROPERTY);
                    sb.append(" name=\"").append(entry.getKey()).append("\">");
                    sb.append(entry.getValue());
                    sb.append("</").append(Extension.Tag.CONFIG_PROPERTY).append(">");
                }
            }

            sb.append("</").append(DsSecurity.Tag.REAUTH_PLUGIN).append(">");
        }

        sb.append("</security>");

        return sb.toString();
    }
}
