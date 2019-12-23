package org.df4j.protocol;

public interface Disposable {
    /**
     *  Dispose the resource, the operation should be idempotent.
     */
    void dispose();

    /**
     * @return true if this resource has been disposed.
     */
    boolean	isDisposed();
}
