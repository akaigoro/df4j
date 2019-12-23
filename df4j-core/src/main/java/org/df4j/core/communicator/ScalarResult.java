package org.df4j.core.communicator;

import org.df4j.protocol.Disposable;
import org.df4j.protocol.Single;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link ScalarResult} can be considered as a one-shot multicast {@link AsyncArrayBlockingQueue}:
 *   once set, it always satisfies {@link ScalarResult#subscribe(Single.Observer)}
 *
 * Universal standalone connector for scalar values.
 * Has synchronous (Future), and asynchronous (both for scalar and stream kinds of subscribers)
 * interfaces on output end.
 */
public class ScalarResult<T> implements Single.Source<T>, Future<T> {
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
    public void subscribe(Single.Observer<? super T> subscriber) {
        bblock.lock();
        try {
            Subscription subscription = new Subscription(subscriber);
            subscriber.onSubscribe(subscription);
        } finally {
            bblock.unlock();
        }
    }

    //@Override
    public void onSuccess(T t) {
        cf.complete(t);
    }

    //@Override
    public void onError(Throwable t) {
        cf.completeExceptionally(t);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return cf.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return cf.get(timeout, unit);
    }

    public T join() {
        return cf.join();
    }

    class Subscription implements Disposable {
        final CompletableFuture<T> whenComplete;

        public Subscription(Single.Observer<? super T> subscriber) {
            this.whenComplete = cf.whenComplete(subscriber);
        }

        @Override
        public void dispose() {
            whenComplete.cancel(true);
        }

        @Override
        public boolean isDisposed() {
            return whenComplete.isCancelled();
        }
    }
}
