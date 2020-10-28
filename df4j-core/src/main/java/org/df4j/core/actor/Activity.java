package org.df4j.core.actor;

import org.df4j.protocol.Completable;

import java.util.concurrent.TimeUnit;

/**
 * methods common to all {@link Node}s and {@link Thread}.
 */
public interface Activity extends Completable.Source {
    /**
     * starts the activity. An Activity may be started only once in lifetime.
     */
    void start();

    /**
     * Tests if this activity is alive. An activity is alive if it has
     * been started and has not yet finished.
     *
     * @return  {@code true} if this activity is alive;
     *          {@code false} otherwise.
     */
    boolean isAlive();

    /**
     *  Awaits the termination of this Completable instance in a blocking manner
     *  and rethrows any exception, if any.
     */
    void await() throws InterruptedException;

    /**
     *  Awaits the termination of this Completable instance in a blocking manner with a specific timeout
     *  and rethrows any exception emitted within the timeout window.
     *
     * @param timeout timeout in milliseconds
     * @return true if this activity has ended.
     */
    boolean await(long timeout) throws InterruptedException;

    /**
     *  Awaits the termination of this Completable instance in a blocking manner with a specific timeout
     *  and rethrows any exception emitted within the timeout window.
     *
     * @param timeout timeout in time units
     * @param unit TimeUnit
     * @return true if this activity has ended.
     */
    default boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return await(unit.toMillis(timeout));
    }
}
