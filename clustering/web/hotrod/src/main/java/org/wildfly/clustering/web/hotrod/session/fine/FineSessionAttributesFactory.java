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

package org.wildfly.clustering.web.hotrod.session.fine;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.hotrod.RemoteCacheEntryMutator;
import org.wildfly.clustering.marshalling.spi.InvalidSerializedFormException;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.SessionAttributes;
import org.wildfly.clustering.web.cache.session.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.fine.FineImmutableSessionAttributes;
import org.wildfly.clustering.web.cache.session.fine.FineSessionAttributes;
import org.wildfly.clustering.web.hotrod.Logger;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * {@link SessionAttributesFactory} for fine granularity sessions.
 * A given session's attributes are mapped to N+1 co-located cache entries, where N is the number of session attributes.
 * A separate cache entry stores the activate attribute names for the session.
 * @author Paul Ferraro
 */
public class FineSessionAttributesFactory<V> implements SessionAttributesFactory<Map<String, UUID>>, BiFunction<SessionAttributeKey, V, Mutator> {

    private final RemoteCache<SessionAttributeNamesKey, Map<String, UUID>> namesCache;
    private final RemoteCache<SessionAttributeKey, V> attributeCache;
    private final Marshaller<Object, V> marshaller;
    private final Immutability immutability;
    private final CacheProperties properties;

    public FineSessionAttributesFactory(RemoteCache<SessionAttributeNamesKey, Map<String, UUID>> namesCache, RemoteCache<SessionAttributeKey, V> attributeCache, Marshaller<Object, V> marshaller, Immutability immutability, CacheProperties properties) {
        this.namesCache = namesCache;
        this.attributeCache = attributeCache;
        this.marshaller = marshaller;
        this.immutability = immutability;
        this.properties = properties;
    }

    @Override
    public Mutator apply(SessionAttributeKey key, V value) {
        return new RemoteCacheEntryMutator<>(this.attributeCache, key, value);
    }

    @Override
    public Map<String, UUID> createValue(String id, Void context) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, UUID> findValue(String id) {
        Map<String, UUID> names = this.namesCache.get(new SessionAttributeNamesKey(id));
        if (names != null) {
            for (Map.Entry<String, UUID> nameEntry : names.entrySet()) {
                V value = this.attributeCache.get(new SessionAttributeKey(id, nameEntry.getValue()));
                if (value != null) {
                    try {
                        this.marshaller.read(value);
                        continue;
                    } catch (InvalidSerializedFormException e) {
                        Logger.ROOT_LOGGER.failedToActivateSessionAttribute(e, id, nameEntry.getKey());
                    }
                } else {
                    Logger.ROOT_LOGGER.missingSessionAttributeCacheEntry(id, nameEntry.getKey());
                }
                this.remove(id);
                return null;
            }
            return names;
        }
        return Collections.emptyMap();
    }

    @Override
    public boolean remove(String id) {
        Map<String, UUID> names = this.namesCache.withFlags(Flag.FORCE_RETURN_VALUE).remove(new SessionAttributeNamesKey(id));
        if (names != null) {
            for (UUID attributeId : names.values()) {
                this.attributeCache.remove(new SessionAttributeKey(id, attributeId));
            }
        }
        return true;
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, Map<String, UUID> names) {
        return new FineSessionAttributes<>(new SessionAttributeNamesKey(id), names, this.namesCache, getKeyFactory(id), this.attributeCache.withFlags(Flag.FORCE_RETURN_VALUE), this.marshaller, this, this.immutability, this.properties);
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, Map<String, UUID> names) {
        return new FineImmutableSessionAttributes<>(names, getKeyFactory(id), this.attributeCache, this.marshaller);
    }

    private static Function<UUID, SessionAttributeKey> getKeyFactory(String id) {
        return new Function<UUID, SessionAttributeKey>() {
            @Override
            public SessionAttributeKey apply(UUID attributeId) {
                return new SessionAttributeKey(id, attributeId);
            }
        };
    }
}
