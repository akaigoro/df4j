package org.df4j.core.dataflow;

import org.df4j.core.communicator.CompletionSubscription;
import org.df4j.protocol.Completable;

import java.util.LinkedList;
import java.util.concurrent.CompletionException;

/**
 * a helper interface to convert a {@link Thread} to {@link Activity}.
 */
public interface ActivityThread extends Activity {
    @Override
    default LinkedList<CompletionSubscription> getSubscriptions() {
        throw new UnsupportedOperationException();
    }

    @Override
    default void subscribe(Completable.Observer co) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Throwable getCompletionException() {
        return null;
    }

    /**
     * Tests if this activity is alive. An activity is alive if it has
     * been started and has not yet finished.
     *
     * @return  {@code true} if this activity is alive;
     *          {@code false} otherwise.
     */
    boolean isAlive();

    void join() throws InterruptedException;

    void join(long timeout)  throws InterruptedException;

    @Override
    default boolean isCompleted() {
        return !isAlive();
    }

    @Override
    default void blockingAwait() throws InterruptedException {
        join();
    }

    @Override
    default boolean blockingAwait(long timeout){
        try {
            join(timeout);
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
        return isCompleted();
    }
}
