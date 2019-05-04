package org.df4j.core.asyncproc;

import io.reactivex.disposables.Disposable;
import org.df4j.core.asyncproc.base.ScalarSubscriptionImpl;
import org.df4j.core.asyncproc.base.ScalarSubscriptionQueue;
import org.df4j.core.asyncproc.base.Stream2ScalarSubscriber;
import org.df4j.core.util.SubscriptionCancelledException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.*;

/**
 * Universal standalone connector for scalar values.
 * Has synchronous (Future), and asynchronous (both for scalar and stream kinds of subscribers)
 * interfaces on output end.
 *
 */
public class ScalarResult<T> implements ScalarSubscriber<T>, ScalarPublisher<T>, Publisher<T>, Future<T> {
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
                onComplete(value);
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
    public synchronized void onComplete(T t) {
        synchronized(this) {
            if (done) { // this is how CompletableFuture#complete works
                return;
            }
            done = true;
            value = t;
            notifyAll();
        }
        subscriptions.onComplete(t);
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
        ScalarSubscriptionImpl subscription = subscriptions.poll();
        for (; subscription != null; subscription = subscriptions.poll()) {
            try {
                subscription.onError(completionException);
            } catch (SubscriptionCancelledException e) {
            }
        }
    }

    public void subscribe(ScalarSubscriber<? super T> s) {
        if (s == null) {
            throw new NullPointerException();
        }
        synchronized(this) {
            if (!isDone()) {
                subscriptions.subscribe(s);
                return;
            }
        }
        if (completionException == null) {
            s.onComplete(value);
        } else {
            s.onError(completionException);
        }
    }

    public void subscribe(Subscriber<? super T> streamSubscriber) {
        if (streamSubscriber == null) {
            throw new NullPointerException();
        }
        synchronized(this) {
            if (!isDone()) {
                subscriptions.subscribe(streamSubscriber);
                return;
            }
        }
        ScalarSubscriber<T> scalarSubscriber = new Stream2ScalarSubscriber<T>(streamSubscriber);
        if (completionException == null) {
            scalarSubscriber.onComplete(value);
        } else {
            scalarSubscriber.onError(completionException);
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
        onComplete(null);
    }

    public static <U> ScalarResult<U> completedResult(U value) {
        ScalarResult<U> result = new ScalarResult<>();
        result.onComplete(value);
        return result;
    }

}
