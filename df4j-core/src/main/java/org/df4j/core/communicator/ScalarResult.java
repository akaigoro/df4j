package org.df4j.core.communicator;

import org.df4j.protocol.ScalarMessage;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link ScalarResult} can be considered as a one-shot {@link AsyncArrayBlockingQueue}: once set,
 * it always satisfies {@link ScalarResult#subscribe(ScalarMessage.Subscriber)}
 *
 * Universal standalone connector for scalar values.
 * Has synchronous (Future), and asynchronous (both for scalar and stream kinds of subscribers)
 * interfaces on output end.
 */
public class ScalarResult<T> implements ScalarMessage.Subscriber<T>, ScalarMessage.Publisher<T>, Future<T> {
    private final Lock bblock = new ReentrantLock();
    protected CompletableFuture<T> cf = new CompletableFuture<>();
    private CompletableFuture<T> subscription;

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
    public void subscribe(ScalarMessage.Subscriber<? super T> s) {
        bblock.lock();
        try {
            if (subscription != null) {
                subscription.cancel(true);
            }
            subscription = cf.whenComplete(s);
        } finally {
            bblock.unlock();
        }
    }

    @Override
    public boolean unsubscribe(ScalarMessage.Subscriber<T> s) {
        bblock.lock();
        try {
            if (subscription == null) {
                return false;
            }
            CompletableFuture<T> sub = subscription;
            subscription = null;
            return sub.cancel(true);
        } finally {
            bblock.unlock();
        }
    }

    @Override
    public void onSuccess(T t) {
        cf.complete(t);
    }

    @Override
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
}
