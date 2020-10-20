package org.df4j.core.actor;

import java.util.concurrent.CompletionException;

/**
 * a helper interface to convert a {@link Thread} to {@link Activity}.
 */
public interface ActivityThread extends Activity {
    void join()  throws InterruptedException;
    void join(long timeout)  throws InterruptedException;

    @Override
    default void await() {
        try {
            join();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    @Override
    default boolean await(long timeout){
        try {
            join(timeout);
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
        return !isAlive();
    }
}
