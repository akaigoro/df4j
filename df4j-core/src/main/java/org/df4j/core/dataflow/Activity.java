package org.df4j.core.dataflow;

import org.df4j.core.communicator.CompletionI;

/**
 * methods common to all {@link Node}s and {@link Thread}.
 */
public interface Activity extends CompletionI {
    /**
     * starts the activity. An Activity may be started only once in lifetime.
     */
    void start();
}
