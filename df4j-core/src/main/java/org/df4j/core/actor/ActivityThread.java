package org.df4j.core.actor;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * a helper interface to convert a {@link Thread} to {@link Activity}.
 */
public class ActivityThread extends Thread implements Activity {

    @Override
    public boolean isCompleted() {
        return !isAlive();
    }

    @Override
    public Throwable getCompletionException() {
        return null;
    }

    @Override
    public void await() {
        try {
            join();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        join(nanos/1000000, (int) (nanos % 1000000));
        return isCompleted();
    }
}
