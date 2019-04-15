package org.df4j.core.asyncproc;

import org.df4j.core.ScalarPublisher;
import org.df4j.core.ScalarSubscriber;
import org.df4j.core.asyncproc.ext.AsyncBiFunction;
import org.df4j.core.asyncproc.ext.AsyncFunction;

import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/** implements Subscriber<T>, CompletionStage<T> {
 * Universal standalone connector for single value.
 * Has both synchronous and asynchronous interfaces on output end.
 *
 * It could named ScalarOutput.
 */
public class AsyncResult<T> implements ScalarSubscriber<T>, ScalarPublisher<T>, CompletionStage<T>, Future<T> {
    private void debug(String s) {
        System.out.println(s);  // must be commented out
    }
    protected ScalarSubscriptionQueue<T> subscriptions = new ScalarSubscriptionQueue<>();
    protected volatile boolean done;
    protected volatile T value;
    protected volatile Throwable completionException;
    /** in case this instance have subscribed to some other Publisher */
    protected ScalarSubscription subscription;

    public AsyncResult() {
    }

    public AsyncResult(CompletionStage<? extends T> completionStage) {
        completionStage.whenComplete((value, ex)->{
            if (ex == null) {
                onComplete(value);
            } else {
                onError(ex);
            }
        });
    }

    @Override
    public void onSubscribe(ScalarSubscription s) {
        subscription = s;
    }

    @Override
    public synchronized void onComplete(T t) {
        synchronized(this) {
            if (done) { // this is how CompletableFuture#complete works
                debug("onNext done already");
                return;
            }
            done = true;
            value = t;
            debug("onNext done, notifyAll");
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
        ScalarSubscription subscription = subscriptions.poll();
        for (; subscription != null; subscription = subscriptions.poll()) {
            subscription.onError(completionException);
        }
    }

    public void subscribe(ScalarSubscriber<? super T> s) {
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

    public void subscribe(CompletableFuture<T> cf) {
        ScalarSubscriber<? super T> s = new CompletableFutureySubscriber<>(cf);
        subscribe(s);
    }

    /**
     * Cancels subscription, and not the task, as interfece {@link Future} assumes.
     * @param mayInterruptIfRunning not used
     * @return false if was not subscribed
     *         true if was subscribed and subscription cancelled
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        ScalarSubscription subscriptionLoc;
        synchronized(this) {
            subscriptionLoc = subscription;
            if (subscriptionLoc == null) {
                return false;
            }
        }
        subscriptionLoc.cancel();
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

    public static <U> AsyncResult<U> completedResult(U value) {
        AsyncResult<U> result = new AsyncResult<>();
        result.onComplete(value);
        return result;
    }

    @Override
    public <U> AsyncResult<U> thenApply(Function<? super T, ? extends U> function) {
        return thenApplyAsync(function, AsyncProc.directExec);
    }

    @Override
    public <U> AsyncResult<U> thenApplyAsync(Function<? super T, ? extends U> function) {
        return thenApplyAsync(function, null);
    }

    @Override
    public <U> AsyncResult<U> thenApplyAsync(Function<? super T, ? extends U> function, Executor executor) {
        AsyncFunction af = new AsyncFunction(function);
        af.setExecutor(executor);
        subscribe(af);
        return af.asyncResult();
    }

    @Override
    public AsyncResult<Void> thenAccept(Consumer<? super T> consumer) {
        return thenAcceptAsync(consumer, AsyncProc.directExec);
    }

    @Override
    public AsyncResult<Void> thenAcceptAsync(Consumer<? super T> consumer) {
        return thenAcceptAsync(consumer, null);
    }

    @Override
    public AsyncResult<Void> thenAcceptAsync(Consumer<? super T> consumer, Executor executor) {
        AsyncFunction af = new AsyncFunction(consumer);
        af.setExecutor(executor);
        subscribe(af);
        return af.asyncResult();
    }

    @Override
    public AsyncResult<Void> thenRun(Runnable runnable) {
        return thenRunAsync(runnable, AsyncProc.directExec);
    }

    @Override
    public AsyncResult<Void> thenRunAsync(Runnable runnable) {
        return thenRunAsync(runnable, null);
    }

    @Override
    public AsyncResult<Void> thenRunAsync(Runnable runnable, Executor executor) {
        AsyncFunction af = new AsyncFunction(runnable);
        af.setExecutor(executor);
        subscribe(af);
        return af.asyncResult();
    }

    @Override
    public <U, V> AsyncResult<V> thenCombine(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> biFunction) {
        return thenCombineAsync(completionStage, biFunction, AsyncProc.directExec);
    }

    @Override
    public <U, V> AsyncResult<V> thenCombineAsync(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> biFunction) {
        return thenCombineAsync(completionStage, biFunction, null);
    }

    @Override
    public <U, V> AsyncResult<V> thenCombineAsync(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> biFunction, Executor executor) {
        AsyncBiFunction af = new AsyncBiFunction(biFunction);
        af.setExecutor(executor);
        this.subscribe(af.param1);
        AsyncResult other = new AsyncResult(completionStage);
        other.subscribe(af.param2);
        return af.asyncResult();
    }

    @Override
    public <U> AsyncResult<Void> thenAcceptBoth(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> biConsumer) {
        return thenAcceptBothAsync(completionStage, biConsumer, AsyncProc.directExec);
    }

    @Override
    public <U> AsyncResult<Void> thenAcceptBothAsync(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> biConsumer) {
        return thenAcceptBothAsync(completionStage, biConsumer, null);
    }

    @Override
    public <U> AsyncResult<Void> thenAcceptBothAsync(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> biConsumer, Executor executor) {
        AsyncBiFunction af = new AsyncBiFunction(biConsumer);
        af.setExecutor(executor);
        this.subscribe(af.param1);
        AsyncResult other = new AsyncResult(completionStage);
        other.subscribe(af.param2);
        return af.asyncResult();
    }

    @Override
    public AsyncResult<Void> runAfterBoth(CompletionStage<?> completionStage, Runnable runnable) {
        return runAfterBothAsync(completionStage, runnable, AsyncProc.directExec);
    }

    @Override
    public AsyncResult<Void> runAfterBothAsync(CompletionStage<?> completionStage, Runnable runnable) {
        return runAfterBothAsync(completionStage, runnable, null);
    }

    @Override
    public AsyncResult<Void> runAfterBothAsync(CompletionStage<?> completionStage, Runnable runnable, Executor executor) {
        AsyncBiFunction af = new AsyncBiFunction(runnable);
        af.setExecutor(executor);
        this.subscribe(af.param1);
        AsyncResult other = new AsyncResult(completionStage);
        other.subscribe(af.param2);
        return af.asyncResult();
    }

    @Override
    public <U> AsyncResult<U> applyToEither(CompletionStage<? extends T> completionStage, Function<? super T, U> function) {
        return null;
    }

    @Override
    public <U> AsyncResult<U> applyToEitherAsync(CompletionStage<? extends T> completionStage, Function<? super T, U> function) {
        return null;
    }

    @Override
    public <U> AsyncResult<U> applyToEitherAsync(CompletionStage<? extends T> completionStage, Function<? super T, U> function, Executor executor) {
        return null;
    }

    @Override
    public AsyncResult<Void> acceptEither(CompletionStage<? extends T> completionStage, Consumer<? super T> consumer) {
        return null;
    }

    @Override
    public AsyncResult<Void> acceptEitherAsync(CompletionStage<? extends T> completionStage, Consumer<? super T> consumer) {
        return null;
    }

    @Override
    public AsyncResult<Void> acceptEitherAsync(CompletionStage<? extends T> completionStage, Consumer<? super T> consumer, Executor executor) {
        return null;
    }

    @Override
    public AsyncResult<Void> runAfterEither(CompletionStage<?> completionStage, Runnable runnable) {
        return null;
    }

    @Override
    public AsyncResult<Void> runAfterEitherAsync(CompletionStage<?> completionStage, Runnable runnable) {
        return null;
    }

    @Override
    public AsyncResult<Void> runAfterEitherAsync(CompletionStage<?> completionStage, Runnable runnable, Executor executor) {
        return null;
    }

    @Override
    public <U> AsyncResult<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> function) {
        return null;
    }

    @Override
    public <U> AsyncResult<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> function) {
        return null;
    }

    @Override
    public <U> AsyncResult<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> function, Executor executor) {
        return null;
    }

    @Override
    public AsyncResult<T> exceptionally(Function<Throwable, ? extends T> function) {
        return null;
    }

    @Override
    public AsyncResult<T> whenComplete(BiConsumer<? super T, ? super Throwable> biConsumer) {
        return whenCompleteAsync(biConsumer, AsyncProc.directExec);
    }

    @Override
    public AsyncResult<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> biConsumer) {
        return whenCompleteAsync(biConsumer, null);
    }

    @Override
    public AsyncResult<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> biConsumer, Executor executor) {
        ScalarSubscriber<? super T> subscriber = new BiconsumerSubscriber<>(biConsumer, executor);
        subscribe(subscriber);
        return null;
    }

    @Override
    public <U> AsyncResult<U> handle(BiFunction<? super T, Throwable, ? extends U> biFunction) {
        return null;
    }

    @Override
    public <U> AsyncResult<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> biFunction) {
        return null;
    }

    @Override
    public <U> AsyncResult<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> biFunction, Executor executor) {
        return null;
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        CompletableFuture<T> res = new CompletableFuture<>();
        this.subscribe(res);
        return res;
    }

    public static class CompletableFutureySubscriber<T> implements ScalarSubscriber<T> {
        private final CompletableFuture<T> cf;

        public CompletableFutureySubscriber(CompletableFuture<T> cf) {
            this.cf = cf;
        }

        @Override
        public void onSubscribe(ScalarSubscription s) {
        }

        @Override
        public void onComplete(T t) {
            cf.complete(t);
        }

        @Override
        public void onError(Throwable t) {
            cf.completeExceptionally(t);
        }
    }

    private class BiconsumerSubscriber<T> extends AsyncProc implements ScalarSubscriber<T> {
        private final BiConsumer<? super T, ? super Throwable> biConsumer;
        private T value;
        private Throwable ex;

        public BiconsumerSubscriber(BiConsumer<? super T, ? super Throwable> biConsumer, Executor executor) {
            this.biConsumer = biConsumer;
            setExecutor(executor);
        }

        @Override
        public void onSubscribe(ScalarSubscription s) {
        }

        @Override
        public void onComplete(T t) {
            asyncRun(t, null);
        }

        @Override
        public void onError(Throwable t) {
            asyncRun(null, t);
        }

        private void asyncRun(T value, Throwable ex) {
            this.value = value;
            this.ex = ex;
            fire();
        }

        @Override
        public void run() {
            biConsumer.accept(value, ex);
        }
    }
}
