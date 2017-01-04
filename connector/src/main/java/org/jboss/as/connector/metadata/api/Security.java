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

package org.jboss.as.connector.metadata.api;

import java.util.HashMap;
import java.util.Map;


/**
 * Security metadata with Elytron support.
 *
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public interface Security extends org.jboss.jca.common.api.metadata.common.Security, SecurityMetadata
{
   /**
    *
    * A Tag.
    *
    */
   public enum Tag
   {
      // new Elytron tags
      /**
       * elytron-security-domain tag
       */
      ELYTRON_SECURITY_DOMAIN("elytron-security-domain"),
      /**
       * security-domain-and-application TAG
       */
      ELYTRON_SECURITY_DOMAIN_AND_APPLICATION("elytron-security-domain-and-application"),

      // tags copied from original tag class
      /** always first
       *
       */
      UNKNOWN(null),

      /**
       * security-domain tag
       */
      SECURITY_DOMAIN("security-domain"),

      /**
       * security-domain-and-application TAG
       */
      SECURITY_DOMAIN_AND_APPLICATION("security-domain-and-application"),

      /**
       * application
       */
      APPLICATION("application");


      private String name;

      /**
       *
       * Create a new Tag.
       *
       * @param name a name
       */
      Tag(final String name) {
         this.name = name;
      }

      /**
       * Get the local name of this element.
       *
       * @return the local name
       */
      public String getLocalName() {
         return name;
      }

      /**
       * {@inheritDoc}
       */
      public String toString() {
         return name;
      }

      private static final Map<String, Tag> MAP;

      static {
         final Map<String, Tag> map = new HashMap<>();
         for (Tag element : values())
         {
            final String name = element.getLocalName();
            if (name != null)
               map.put(name, element);
         }
         MAP = map;
      }

      /**
       * Set the value
       * @param v The name
       * @return The value
       */
      Tag value(String v) {
         name = v;
         return this;
      }

      /**
       *
       * Static method to get enum instance given localName XsdString
       *
       * @param localName a XsdString used as localname (typically tag name as defined in xsd)
       * @return the enum instance
       */
      public static Tag forName(String localName) {
         final Tag element = MAP.get(localName);
         return element == null ? UNKNOWN.value(localName) : element;
      }

   }
}
