package org.df4j.core.actor;

import java.util.concurrent.CompletionException;

/**
 * a helper interface to convert a {@link Thread} to {@link Activity}.
 */
public class ActivityThread extends Thread implements Activity {

    @Override
    public void await() {
        try {
            join();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    @Override
    public boolean await(long timeout){
        try {
            join(timeout);
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
        return !isAlive();
    }
}
