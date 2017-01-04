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

import org.jboss.jca.common.api.validator.ValidateException;

/**
 * CredentialImpl metadata with Elytron support.
 *
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public class CredentialImpl
        extends org.jboss.jca.common.metadata.common.CredentialImpl implements org.jboss.as.connector.metadata.api.Credential {

    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Indicates if the Credential data belongs to Elytron or PicketBox.
     */
    private boolean elytronEnabled;

    /**
     * Create a new CredentialImpl.
     *
     * @param userName userName
     * @param password password
     * @param securityDomain securityDomain
     * @param elytronEnabled does the security domain belongs to Elytron
     * @throws ValidateException ValidateException
     */
    public CredentialImpl(String userName, String password, String securityDomain, boolean elytronEnabled)
            throws ValidateException {
        super(userName, password, securityDomain);
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
        if (!(obj instanceof CredentialImpl))
            return false;
        CredentialImpl other = (CredentialImpl) obj;
        return elytronEnabled == other.elytronEnabled && super.equals(other);
    }
}
