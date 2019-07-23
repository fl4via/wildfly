/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;

/**
 * Scheduler for eager eviction of a bean.
 * @author Paul Ferraro
 * @author Flavia Rainone
 */
public class EagerEvictionScheduler<I, T> implements Scheduler<I>, BeanGroupEvictor<I>, Consumer<I> {

    private final BeanFactory<I, T> factory;
    private final ScheduledExecutorService executor;
    private final Duration idleTimeout;
    private final ExpirationTracker<I> expirationTracker;
    private final Map<I, I> groupIdMap = new HashMap<>();
    private Future<?> evictionTask;

    private final CommandDispatcher<BeanGroupEvictor<I>> dispatcher;

    EagerEvictionScheduler(BeanFactory<I, T> factory, BeanGroupEvictor<I> evictor, ScheduledExecutorService executor, Duration idleTimeout, CommandDispatcherFactory dispatcherFactory, String dispatcherName) {
        this.factory = factory;
        this.executor = executor;
        this.idleTimeout = idleTimeout;
        this.expirationTracker = new ExpirationTracker<>(idleTimeout);
        this.dispatcher = dispatcherFactory.createCommandDispatcher(dispatcherName + "/eviction", evictor);
    }

    @Override
    public void accept(I id) {
        this.expirationTracker.forget(id);
    }

    @Override
    public void schedule(I id) {
        BeanEntry<I> entry = this.factory.findValue(id);
        if (entry != null) {
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Scheduling stateful session bean %s to passivate in %s", id, this.idleTimeout);
            expirationTracker.trackExpiration(id);
            if (!groupIdMap.containsKey(id)) {
                groupIdMap.put(id, entry.getGroupId());
            }
            // Make sure the map insertion happens before map removal (during task execution).
            synchronized (this) {
                if (evictionTask != null) {
                    Runnable task = new EvictionTask();
                    evictionTask = this.executor.schedule(task, this.idleTimeout.toMillis(), TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    @Override
    public void cancel(I id) {
        expirationTracker.forget(id);
        groupIdMap.remove(id);
    }

    @Override
    public void cancel(Locality locality) {
        for (I id: this.expirationTracker.getTrackedIds()) {
            if (Thread.currentThread().isInterrupted()) break;
            if (!locality.isLocal(id)) {
                this.cancel(id);
            }
        }
    }

    @Override
    public void close() {
        final Future<?> task;
        synchronized (this) {
            if (this.evictionTask == null) {
                return;
            }
            task = evictionTask;

        }
        task.cancel(false);
        if (!task.isDone()) {
            try {
                task.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                // Ignore
            }
        }
        this.dispatcher.close();
    }

    @Override
    public void evict(I id) {
        try {
            // Cache eviction is a local operation, so we need to broadcast this to the cluster
            this.dispatcher.executeOnGroup(new EvictCommand<>(id));
        } catch (CommandDispatcherException e) {
            InfinispanEjbLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    static class EvictCommand<I> implements Command<Void, BeanGroupEvictor<I>> {
        private static final long serialVersionUID = -7382608648983713382L;

        private final I id;

        EvictCommand(I id) {
            this.id = id;
        }

        @Override
        public Void execute(BeanGroupEvictor<I> evictor) {
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Passivating stateful session bean %s", this.id);
            evictor.evict(this.id);
            return null;
        }
    }

    private class EvictionTask implements Runnable {

        @Override
        public void run() {
            I sessionId;
            final long currentTime = System.currentTimeMillis();
            while ((sessionId = expirationTracker.getExpiredId(currentTime)) != null) {
                InfinispanEjbLogger.ROOT_LOGGER.tracef("Evicting stateful session bean %s", sessionId);
                I groupId = groupIdMap.remove(sessionId);
                try {
                    evict(groupId);
                } finally {
                    synchronized (EagerEvictionScheduler.this) {
                        accept(sessionId);
                    }
                }
            }
            final long nextExpirationInMillis = expirationTracker.getNextExpirationInMillis();
            synchronized (EagerEvictionScheduler.this) {
                if (nextExpirationInMillis != -1) {
                    evictionTask = executor.schedule(this, nextExpirationInMillis, TimeUnit.MILLISECONDS);
                } else {
                    evictionTask = null;
                }
            }

        }
    }
}
