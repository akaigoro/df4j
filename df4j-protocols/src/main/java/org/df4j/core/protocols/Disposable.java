package org.df4j.core.protocols;

/**
 * Represents a disposable resource.
 */
public interface Disposable {

    /**
     * Requests the {@link Scalar.Publisher} to stop sending data and clean up resources.
     * <p>
     * Data may still be sent to meet previously signalled demand after calling cancel.
     */
    public void dispose();

    /**
     * Returns true if this resource has been disposed.
     * @return true if this resource has been disposed
     */
    public boolean isDisposed();
}
