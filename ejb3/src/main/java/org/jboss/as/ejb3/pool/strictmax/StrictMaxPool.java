/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.pool.strictmax;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.pool.AbstractPool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A pool with a maximum size.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public class StrictMaxPool<T> extends AbstractPool<T> {



    /**
     * A FIFO semaphore that is set when the strict max size behavior is in effect.
     * When set, only maxSize instances may be active and any attempt to get an
     * instance will block until an instance is freed.
     */
    private final Semaphore semaphore;
    /**
     * The maximum number of instances allowed in the pool
     */
    private final int maxSize;
    /**
     * The time to wait for the semaphore.
     */
    private final long timeout;
    private final TimeUnit timeUnit;
    /**
     * The pool data structure
     * Guarded by the implicit lock for "pool"
     */
    private final Queue<T> pool = new ConcurrentLinkedQueue<T>();

    public StrictMaxPool(StatelessObjectFactory<T> factory, int maxSize, long timeout, TimeUnit timeUnit) {
        super(factory);
        this.maxSize = maxSize;
        this.semaphore = new Semaphore(maxSize, false);
        this.timeout = TimeUnit.NANOSECONDS.convert(timeout, timeUnit);
        this.timeUnit = timeUnit;
    }

    public void discard(T ctx) {
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.tracef("Discard instance %s#%s", this, ctx);
        }

        // If we block when maxSize instances are in use, invoke release on strictMaxSize
        semaphore.release();

        // Let the super do any other remove stuff
        super.doRemove(ctx);
    }

    public int getCurrentSize() {
        return getCreateCount() - getRemoveCount();
    }

    public int getAvailableCount() {
        return semaphore.availablePermits();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        throw EjbLogger.ROOT_LOGGER.methodNotImplemented();
    }

    /**
     * Get an instance without identity.
     * Can be used by finders,create-methods, and activation
     *
     * @return Context /w instance
     */
    public T get() {
        try {
            boolean acquired = semaphore.tryAcquire(timeout, TimeUnit.NANOSECONDS);
            if (!acquired)
                throw EjbLogger.ROOT_LOGGER.failedToAcquirePermit(timeUnit.convert(timeout, TimeUnit.NANOSECONDS), timeUnit);
        } catch (InterruptedException e) {
            throw EjbLogger.ROOT_LOGGER.acquireSemaphoreInterrupted();
        }

        T bean = pool.poll();

        if( bean !=null) {
            //we found a bean instance in the pool, return it
            return bean;
        }

        try {
            // Pool is empty, create an instance
            bean = create();
        } finally {
            if (bean == null) {
                semaphore.release();
            }
        }
        return bean;
    }

    /**
     * Return an instance after invocation.
     * <p/>
     * Called in 2 cases:
     * a) Done with finder method
     * b) Just removed
     *
     * @param obj
     */
    public void release(T obj) {
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.tracef("%s/%s Free instance: %s", pool.size(), maxSize, this);
        }

        pool.add(obj);

        semaphore.release();
    }

    @Override
    @Deprecated
    public void remove(T ctx) {
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.tracef("Removing instance: %s#%s", this, ctx);
        }

        semaphore.release();
        // let the super do the other remove stuff
        super.doRemove(ctx);
    }

    public void start() {
        // TODO Auto-generated method stub

    }

    public void stop() {
        for (T obj = pool.poll(); obj != null; obj = pool.poll()) {
            destroy(obj);
        }
    }

    /**
     * Non-fair lock-free Semaphore with customizable back-off strategy for high
     * contention scenarios.
     *
     * @author user2296177
     * @version 1.0
     *
     */

}

class MySemaphore {
    /**
     * Default back-off strategy to prevent busy-wait loop. Calls
     * Thread.sleep(0, 1);. Has better performance and lower CPU usage than
     * Thread.yield() inside busy-wait loop.
     */
    private static Runnable defaultBackoffStrategy = () -> {
        try {
            Thread.sleep( 0, 1 );
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }
    };

    private AtomicInteger permitCount;
    private final Runnable backoffStrategy;

    /**
     * Construct a Semaphore instance with maxPermitCount permits and the
     * default back-off strategy.
     *
     * @param maxPermitCount
     *            Maximum number of permits that can be distributed.
     */
    public MySemaphore( final int maxPermitCount ) {
        this( maxPermitCount, defaultBackoffStrategy );
    }

    /**
     * Construct a Semaphore instance with maxPermitCount permits and a custom
     * Runnable to run a back-off strategy during contention.
     *
     * @param maxPermitCount
     *            Maximum number of permits that can be distributed.
     * @param backoffStrategy
     *            Runnable back-off strategy to run during high contention.
     */
    public MySemaphore( final int maxPermitCount, final Runnable backoffStrategy ) {
        permitCount = new AtomicInteger( maxPermitCount );
        this.backoffStrategy = backoffStrategy;
    }

    /**
     * Attempt to acquire one permit and immediately return.
     *
     * @return true : acquired one permits.<br>
     *         false: did not acquire one permit.
     */
    public boolean tryAcquire() {
        return tryAcquire( 1 );
    }

    /**
     * Attempt to acquire n permits and immediately return.
     *
     * @param n
     *            Number of permits to acquire.
     * @return true : acquired n permits.<br>
     *         false: did not acquire n permits.
     */
    public boolean tryAcquire( final int n ) {
        return tryDecrementPermitCount( n );
    }

    /**
     * Acquire one permit.
     */
    public void acquire() {
        acquire( 1 );
    }

    public int availablePermits() {
        return permitCount.get();
    }

    /**
     * Acquire n permits.
     *
     * @param n
     *            Number of permits to acquire.
     */
    public void acquire( final int n ) {
        while ( !tryDecrementPermitCount( n ) ) {
            backoffStrategy.run();
        }
    }

    public boolean tryAcquire( long timeout, TimeUnit timeUnit) {
        final long currentTime = System.nanoTime();
        while ( !tryDecrementPermitCount(1) ) {
            if (System.nanoTime() - currentTime >= timeout) {
                return false;
            }
            backoffStrategy.run();
        }
        return true;
    }

    /**
     * Release one permit.
     */
    public void release() {
        release( 1 );
    }

    /**
     * Release n permits.
     *
     * @param n
     *            Number of permits to release.
     */
    public void release( final int n ) {
        permitCount.addAndGet( n );
    }

    /**
     * Try decrementing the current number of permits by n.
     *
     * @param n
     *            The number to decrement the number of permits.
     * @return true : the number of permits was decremented by n.<br>
     *         false: decrementing the number of permits results in a negative
     *         value or zero.
     */
    private boolean tryDecrementPermitCount( final int n ) {
        int oldPermitCount;
        int newPermitCount;
        do {
            oldPermitCount = permitCount.get();
            newPermitCount = oldPermitCount - n;
            //if ( newPermitCount > n ) throw new ArithmeticException( "Overflow" );
            if ( /*oldPermitCount == 0 || */newPermitCount < 0 ) return false;
        } while ( !permitCount.compareAndSet( oldPermitCount, newPermitCount ) );
        return true;
    }
}