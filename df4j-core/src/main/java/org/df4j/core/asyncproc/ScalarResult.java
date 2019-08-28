package org.df4j.core.asyncproc;

import org.df4j.core.asyncproc.base.ScalarSubscriptionQueue;
import org.df4j.core.protocols.Disposable;
import org.df4j.core.protocols.Scalar;
import org.df4j.core.util.SubscriptionCancelledException;

import java.util.concurrent.*;

/**
 * Universal standalone connector for scalar values.
 * Has synchronous (Future), and asynchronous (both for scalar and stream kinds of subscribers)
 * interfaces on output end.
 *
 */
public class ScalarResult<T> implements Scalar.Subscriber<T>, Promise<T> {
    protected AsyncProc<T> parent;
    protected ScalarSubscriptionQueue<T> subscriptions = new ScalarSubscriptionQueue<>();
    protected volatile boolean done;
    protected volatile T value;
    protected volatile Throwable completionException;
    /** in case this instance have subscribed to some other Publisher */
    protected Disposable subscription;

    public ScalarResult(AsyncProc<T> parent) {
        this.parent = parent;
    }

    public ScalarResult() {
    }

    public ScalarResult(CompletionStage<? extends T> completionStage) {
        completionStage.whenComplete((value, ex)->{
            if (ex == null) {
                onSuccess(value);
            } else {
                onError(ex);
            }
        });
    }

    protected void debug(String s) {
        //       System.out.println(s);  // must be commented out
    }

    @Override
    public void onSubscribe(Disposable s) {
        subscription = s;
    }

    @Override
    public synchronized void onSuccess(T t) {
        synchronized(this) {
            if (done) { // this is how CompletableFuture#complete works
                return;
            }
            done = true;
            value = t;
            notifyAll();
        }
        subscriptions.onSuccess(t);
    }

    @Override
    public synchronized void onError(Throwable t) {
        synchronized(this) {
            if (done) { // this is how CompletableFuture#completeExceptionally works
                return;
            }
            done = true;
            completionException = t;
            notifyAll();
        }
        ScalarSubscriptionQueue.ScalarSubscriptionImpl subscription = subscriptions.poll();
        for (; subscription != null; subscription = subscriptions.poll()) {
            try {
                subscription.onError(completionException);
            } catch (SubscriptionCancelledException e) {
            }
        }
    }

    public void subscribe(Scalar.Subscriber<? super T> s) {
        if (s == null) {
            throw new NullPointerException();
        }
        synchronized(this) {
            if (!isDone()) {
                if (s == null) {
                    throw new NullPointerException();
                }
                subscriptions.subscribe(s);
                return;
            }
        }
        if (completionException == null) {
            s.onSuccess(value);
        } else {
            s.onError(completionException);
        }
    }

    /**
     * Cancels subscription, and not the task, as interfece {@link Future} assumes.
     * @param mayInterruptIfRunning not used
     * @return false if was not subscribed
     *         true if was subscribed and subscription cancelled
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        Disposable subscriptionLoc;
        synchronized(this) {
            subscriptionLoc = subscription;
            if (subscriptionLoc == null) {
                return false;
            }
        }
        subscriptionLoc.dispose();
        return true;
    }

    /**
     *
     * @return true if not subscribed
     */
    @Override
    public synchronized boolean isCancelled() {
        return subscription == null;
    }

    @Override
    public synchronized boolean isDone() {
        return done;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        while (!done) {
            wait();
        }
        if (completionException == null) {
            return value;
        } else {
            throw new ExecutionException(completionException);
        }
    }

    @Override
    public synchronized T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long millis = unit.toMillis(timeout);
        long targetTime = System.currentTimeMillis()+ millis;
        while (!done) {
            debug("get !done, wait");
            wait(millis);
            if (done) {
                break;
            }
            millis = targetTime - System.currentTimeMillis();
            if (millis <= 0) {
                throw new TimeoutException();
            }
        }
        if (completionException == null) {
            debug("get done, value");
            return value;
        } else {
            debug("get done, throw");
            throw new ExecutionException(completionException);
        }
    }

    public void onComplete() {
        onSuccess(null);
    }

    public static <U> ScalarResult<U> completedResult(U value) {
        ScalarResult<U> result = new ScalarResult<>();
        result.onSuccess(value);
        return result;
    }

}
