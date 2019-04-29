package org.df4j.core.asyncproc;

/**
 * Basic class for all pins and connectors (places for tokens).
 * Can be considered as an asynchronous binary semaphore.
 * Has 2 states: blocked or unblocked.
 * When all pins become unblocked, method {@link Transition#fire()} is called.
 * This resembles firing of a Petri Net transition.
 */
public class ScalarLock {
    protected Transition transition;
    protected boolean completed = false;

    /**
     * initially in blocked state
     */
    public ScalarLock(Transition transition) {
        this.transition = transition;
        transition.register(this);
    }

    public boolean isParameter() {
        return false;
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    protected void complete() {
        synchronized (this) {
            if (completed) {
                return;
            }
            completed = true;
        }
        transition.decBlocked();
    }

    public boolean isBlocked() {
        return !completed;
    }

    /** must be overriden in parameters
     *
     * @return current value of the parameter
     */
    public Object current() {
        throw new UnsupportedOperationException();
    }

    /**
     * this method is used in actors only, being called after next step execution.
     * Scalar pins are completed by that moment, and so cannot be moved.
     *
     * @return false
     */
    public boolean moveNext() {
        return false;
    }
}
