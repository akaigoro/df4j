package org.df4j.core.communicator;

import org.df4j.protocol.Scalar;
import org.df4j.protocol.ScalarSubscription;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link ScalarResult} can be considered as a one-shot multicast {@link AsyncArrayBlockingQueue}:
 *   once set, it always satisfies {@link ScalarResult#subscribe(Scalar.Observer)}
 * <p>
 * Universal standalone connector for scalar values.
 * Has synchronous (Future), and asynchronous (both for scalar and stream kinds of subscribers)
 * interfaces on output end.
 *  an equivalent to {@link CompletableFuture}&lt;{@link T}&gt;
 *
 * @param <T> the type of completion value
 */
public class ScalarResult<T> implements Scalar.Source<T>, Future<T> {
    private final Lock bblock = new ReentrantLock();
    protected final CompletableFuture<T> cf = new CompletableFuture<>();

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return cf.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return cf.isCancelled();
    }

    @Override
    public boolean isDone() {
        return cf.isDone();
    }

    @Override
    public void subscribe(Scalar.Observer<? super T> subscriber) {
        bblock.lock();
        try {
            Subscription subscription = new Subscription(subscriber);
            subscriber.onSubscribe(subscription);
        } finally {
            bblock.unlock();
        }
    }

    /**
     * completes this {@link ScalarResult} with value
     * @param message completion value
     */
    public void onSuccess(T message) {
        cf.complete(message);
    }

    /**
     * completes this {@link ScalarResult} exceptionally
      * @param exception completion exception
     */
    public void onError(Throwable exception) {
        cf.completeExceptionally(exception);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return cf.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return cf.get(timeout, unit);
    }

    /**
     * Returns the result value when complete, or throws an
     * (unchecked) exception if completed exceptionally.
     *
     * @return the result value
     * @throws CompletionException if this  {@link ScalarResult} completed exceptionally
     */
    public T join() {
        return cf.join();
    }

    class Subscription implements ScalarSubscription {
        final CompletableFuture<T> whenComplete;

        public Subscription(Scalar.Observer<? super T> subscriber) {
            this.whenComplete = cf.whenComplete(subscriber);
        }

        @Override
        public void cancel() {
            whenComplete.cancel(true);
        }

        @Override
        public boolean isCancelled() {
            return whenComplete.isCancelled();
        }
    }
}
