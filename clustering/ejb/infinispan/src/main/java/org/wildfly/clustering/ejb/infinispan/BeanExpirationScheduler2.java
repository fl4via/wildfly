/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;

/**
 * Schedules a bean for expiration.
 *
 * @author Paul Ferraro
 *
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
// can this be scheduled as a single task for a bunch of beans?
public class BeanExpirationScheduler2<I, T> implements Scheduler<I> {
    final Batcher<TransactionBatch> batcher;
    final BeanRemover<I, T> remover;
    final ExpirationConfiguration<T> expiration;
    final DurationMap<I> durationMap;
    Future<?> expireTask;

    public BeanExpirationScheduler2(Batcher<TransactionBatch> batcher, BeanRemover<I, T> remover, ExpirationConfiguration<T> expiration) {
        this.batcher = batcher;
        this.remover = remover;
        this.expiration = expiration;
        this.durationMap = expiration.getTimeout().isNegative()? null : new DurationMap(expiration.getTimeout());
    }

    @Override
    public void schedule(I id) {
        Duration timeout = this.expiration.getTimeout();
        if (durationMap != null) {
            durationMap.resetExpiration(id);
            if (expireTask != null) {
                System.out.println("SCHEDULE EXPIRATION bean id " + id);
                InfinispanEjbLogger.ROOT_LOGGER
                        .tracef("Scheduling stateful session bean %s to expire in %s",
                                id, timeout);
                Runnable task = new ExpirationTask();
                // Make sure the expiration future map insertion happens before map removal (during task execution).
                synchronized (this) {
                    expireTask = this.expiration.getExecutor().schedule(task, timeout.toMillis(), TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    @Override
    public void cancel(I id) {
        new Exception("CANCEL EXPIRATION bean id " + id).printStackTrace();
        /*Future<?> future = this.expirationFutures.remove(id);
        if (future != null) {
            future.cancel(false);
        }*/
        this.durationMap.resetExpiration(id);
    }

    @Override
    public void cancel(Locality locality) {
        for (I id: durationMap.getSessionIds()) {
            if (Thread.currentThread().isInterrupted()) break;
            if (!locality.isLocal(id)) {
                this.cancel(id);
            }
        }
    }

    @Override
    public void close() {
        expireTask.cancel(false);
        if (!expireTask.isDone()) {
            try {
                expireTask.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                // Ignore
            }
        }
    }

    private class ExpirationTask implements Runnable {


        ExpirationTask() {

        }

        @Override
        public void run() {
            I sessionId;
            boolean removed = true;
            while ((sessionId = durationMap.getExpiredSessionId()) != null) {
                InfinispanEjbLogger.ROOT_LOGGER.tracef("Expiring stateful session bean %s", sessionId);
                removed = false;
                try (Batch batch = BeanExpirationScheduler2.this.batcher.createBatch()) {
                    try {
                        removed = BeanExpirationScheduler2.this.remover.remove(sessionId, BeanExpirationScheduler2.this.expiration.getRemoveListener());
                    } catch (Throwable e) {
                        InfinispanEjbLogger.ROOT_LOGGER.failedToExpireBean(e, sessionId);
                        batch.discard();
                    }
                }

            }
            if (!removed) {
                // If bean failed to expire, likely due to a lock timeout, just reschedule it
                durationMap.retryExpiration(sessionId);
            }
            final long nextExpirationInMillis = durationMap.getNextExpirationInMillis();
            synchronized (BeanExpirationScheduler2.this) {
                if (nextExpirationInMillis != -1) {
                    expireTask = expiration.getExecutor().schedule(this, nextExpirationInMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                } else {
                    expireTask = null;
                }
            }

        }
    }
}
