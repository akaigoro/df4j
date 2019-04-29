package org.df4j.core.actor.base;

import org.df4j.core.asyncproc.base.ScalarLock;
import org.df4j.core.asyncproc.base.Transition;

/**
 * Basic class for all pins and connectors (places for tokens).
 * Can be considered as an asynchronous binary semaphore.
 * Has 2 states: blocked or unblocked.
 * When all pins become unblocked, method {@link Transition#fire()} is called.
 * This resembles firing of a Petri Net transition.
 */
public class StreamLock extends ScalarLock {
    private boolean blocked = true;

    /**
     * initially in blocked state
     */
    public StreamLock(Transition transition) {
        super(transition);
    }

    public boolean isParameter() {
        return false;
    }

    public boolean isBlocked() {
        return blocked && !completed;
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    /**
     * pins the pin
     * called when a token is consumed and the pin become empty
     */
    public void block() {
        synchronized (this) {
            if (completed) {
                return;
            }
            if (blocked) {
                return;
            }
            blocked = true;
        }
        transition.incBlocked();
    }

    public void unblock() {
        synchronized (this) {
            if (completed) {
                return;
            }
            if (!blocked) {
                return;
            }
            blocked = false;
        }
        transition.decBlocked();
    }
}
