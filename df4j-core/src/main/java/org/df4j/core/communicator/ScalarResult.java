package org.df4j.core.communicator;

import org.df4j.core.protocol.ScalarMessage;

import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * {@link ScalarResult} can be considered as a one-shot {@link AsyncArrayQueue}: once set,
 * it always satisfies {@link ScalarResult#subscribe(ScalarMessage.Subscriber)}
 *
 * Universal standalone connector for scalar values.
 * Has synchronous (Future), and asynchronous (both for scalar and stream kinds of subscribers)
 * interfaces on output end.
 */
public class ScalarResult<T> implements ScalarMessage.Subscriber<T>, ScalarMessage.Publisher<T>, Future<T> {
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
    public synchronized void subscribe(ScalarMessage.Subscriber<? super T> s) {
        if (subscription != null) {
            subscription.cancel(true);
        }
        subscription = cf.whenComplete(s);
    }

    @Override
    public synchronized boolean unsubscribe(ScalarMessage.Subscriber<T> s) {
        if (subscription == null) {
            return false;
        }
        CompletableFuture<T> sub = subscription;
        subscription = null;
        return sub.cancel(true);
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
