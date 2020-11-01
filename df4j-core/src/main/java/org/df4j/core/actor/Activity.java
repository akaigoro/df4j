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
}
