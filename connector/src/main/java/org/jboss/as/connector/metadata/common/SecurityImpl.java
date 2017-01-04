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

import org.jboss.as.connector.metadata.api.Security;

import org.jboss.jca.common.api.validator.ValidateException;


/**
 * SecurityImpl metadata with Elytron support.
 *
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public class SecurityImpl extends org.jboss.jca.common.metadata.common.SecurityImpl implements Security {

    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    private boolean elytronEnabled;

   /* *//**
     * The Elytron Security Domain
     *//*
    private String elytronSecurityDomain;

    *//**
     * The Elytron Security Domain And Application
     *//*
    private String elytronSecurityDomainAndApplication;*/

    /**
     * Constructor
     *
     * @param securityDomain                      securityDomain managed
     * @param securityDomainAndApplication        securityDomainAndApplication managed
     * @param applicationManaged                  applicationManaged
     * @param elytronEnabled                      indicates if the security domain information belongs to PicketBox or Elytron
     * @throws ValidateException ValidateException
     */
    public SecurityImpl(String securityDomain, String securityDomainAndApplication, boolean applicationManaged, boolean elytronEnabled) throws ValidateException {
        super(securityDomain, securityDomainAndApplication, applicationManaged);
        this.elytronEnabled = elytronEnabled;
    }

    @Override
    public boolean isElytronEnabled() {
        return elytronEnabled;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return super.hashCode() + (elytronEnabled? 1: 0);
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (obj instanceof SecurityImpl) {
            SecurityImpl other = (SecurityImpl) obj;
            if (elytronEnabled != other.elytronEnabled)
                return false;
        }
        return super.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        if (elytronEnabled) {
            StringBuilder sb = new StringBuilder(1024);

            sb.append("<security>");

            if (getSecurityDomain() != null) {
                sb.append("<").append(Security.Tag.ELYTRON_SECURITY_DOMAIN).append("/>");
                sb.append(getSecurityDomain());
                sb.append("</").append(Security.Tag.ELYTRON_SECURITY_DOMAIN).append("/>");
            } else {
                sb.append("<").append(Security.Tag.ELYTRON_SECURITY_DOMAIN_AND_APPLICATION).append("/>");
                sb.append(getSecurityDomainAndApplication());
                sb.append("</").append(Security.Tag.ELYTRON_SECURITY_DOMAIN_AND_APPLICATION).append("/>");
            }
            sb.append("</security>");

        }
        return super.toString();
    }
}
