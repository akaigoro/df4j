package org.df4j.core.actor;

import org.df4j.core.asyncproc.Transition;

public abstract class StreamParameter<T> extends StreamLock {

    public StreamParameter(Transition transition) {
        super(transition);
    }

    @Override
    public boolean isParameter() {
        return true;
    }

    /** must be overriten
     *
     * @return true if next element is obtained and this pin is unblocked.
     *         false if next element is unavailable for now,
     *         and this pin becomes blocked.
     */
    public boolean moveNext() {
        throw new UnsupportedOperationException();
    }

    public T current() {
        return getCurrent();
    }

    public abstract T getCurrent();
}
