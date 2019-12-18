package org.df4j.core.actor;

import java.util.concurrent.CompletionException;

/**
 * a helper interface to convert a {@link Thread} to {@link Activity}.
 */
public interface ActivityThread extends Activity {
    public void join(long timeout)  throws InterruptedException;

    @Override
    default boolean blockingAwait(long timeout){
        try {
            join(timeout);
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
        return !isAlive();
    }
}
