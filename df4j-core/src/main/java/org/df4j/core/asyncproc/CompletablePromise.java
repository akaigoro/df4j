package org.df4j.core.asyncproc;

import org.df4j.core.asyncproc.base.ScalarSubscription;
import org.df4j.core.asyncproc.base.ScalarSubscriptionQueue;
import org.df4j.core.asyncproc.base.Stream2ScalarSubscriber;
import org.df4j.core.asyncproc.ext.AsyncBiFunction;
import org.df4j.core.asyncproc.ext.AsyncFunction;
import org.df4j.core.util.SubscriptionCancelledException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Universal standalone connector for scalar values.
 * Has synchronous (Future), and asynchronous (both for scalar and stream kinds of subscribers)
 * interfaces on output end.
 *
 */
public class CompletablePromise<T> implements ScalarSubscriber<T>, ScalarPublisher<T>, Publisher<T>, Future<T>, CompletionStage<T> {
    protected AsyncProc<T> parent;
    protected ScalarSubscriptionQueue<T> subscriptions = new ScalarSubscriptionQueue<>();
    protected volatile boolean done;
    protected volatile T value;
    protected volatile Throwable completionException;
    /** in case this instance have subscribed to some other Publisher */
    protected ScalarSubscription subscription;

    public CompletablePromise(AsyncProc<T> parent) {
        this.parent = parent;
    }

    public CompletablePromise() {
    }

    public CompletablePromise(CompletionStage<? extends T> completionStage) {
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
    public void onSubscribe(ScalarSubscription s) {
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
        ScalarSubscription subscription = subscriptions.poll();
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

    public void subscribe(CompletableFuture<? super T> cf) {
        if (cf == null) {
            throw new NullPointerException();
        }
        synchronized(this) {
            if (!isDone()) {
                subscriptions.subscribe(cf);
                return;
            }
        }
        if (completionException == null) {
            cf.complete(value);
        } else {
            cf.completeExceptionally(completionException);
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

    public static <U> CompletablePromise<U> completedResult(U value) {
        CompletablePromise<U> result = new CompletablePromise<>();
        result.onComplete(value);
        return result;
    }

    @Override
    public <U> CompletablePromise<U> thenApply(Function<? super T, ? extends U> function) {
        return thenApplyAsync(function, AsyncProc.directExec);
    }

    @Override
    public <U> CompletablePromise<U> thenApplyAsync(Function<? super T, ? extends U> function) {
        return thenApplyAsync(function, null);
    }

    @Override
    public <U> CompletablePromise<U> thenApplyAsync(Function<? super T, ? extends U> function, Executor executor) {
        AsyncFunction af = new AsyncFunction(function);
        af.setExecutor(executor);
        subscribe(af);
        return af.asyncResult();
    }

    @Override
    public CompletablePromise<Void> thenAccept(Consumer<? super T> consumer) {
        return thenAcceptAsync(consumer, AsyncProc.directExec);
    }

    @Override
    public CompletablePromise<Void> thenAcceptAsync(Consumer<? super T> consumer) {
        return thenAcceptAsync(consumer, null);
    }

    @Override
    public CompletablePromise<Void> thenAcceptAsync(Consumer<? super T> consumer, Executor executor) {
        AsyncFunction af = new AsyncFunction(consumer);
        af.setExecutor(executor);
        subscribe(af);
        return af.asyncResult();
    }

    @Override
    public CompletablePromise<Void> thenRun(Runnable runnable) {
        return thenRunAsync(runnable, AsyncProc.directExec);
    }

    @Override
    public CompletablePromise<Void> thenRunAsync(Runnable runnable) {
        return thenRunAsync(runnable, null);
    }

    @Override
    public CompletablePromise<Void> thenRunAsync(Runnable runnable, Executor executor) {
        AsyncFunction af = new AsyncFunction(runnable);
        af.setExecutor(executor);
        subscribe(af);
        return af.asyncResult();
    }

    @Override
    public <U, V> CompletablePromise<V> thenCombine(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> biFunction) {
        return thenCombineAsync(completionStage, biFunction, AsyncProc.directExec);
    }

    @Override
    public <U, V> CompletablePromise<V> thenCombineAsync(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> biFunction) {
        return thenCombineAsync(completionStage, biFunction, null);
    }

    @Override
    public <U, V> CompletablePromise<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> biFunction, Executor executor) {
        AsyncBiFunction af = new AsyncBiFunction(biFunction);
        af.setExecutor(executor);
        this.subscribe(af.param1);
        other.whenComplete(af.param2);
        return af.asyncResult();
    }

    @Override
    public <U> CompletablePromise<Void> thenAcceptBoth(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> biConsumer) {
        return thenAcceptBothAsync(completionStage, biConsumer, AsyncProc.directExec);
    }

    @Override
    public <U> CompletablePromise<Void> thenAcceptBothAsync(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> biConsumer) {
        return thenAcceptBothAsync(completionStage, biConsumer, null);
    }

    @Override
    public <U> CompletablePromise<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> biConsumer, Executor executor) {
        AsyncBiFunction af = new AsyncBiFunction(biConsumer);
        af.setExecutor(executor);
        this.subscribe(af.param1);
        other.whenComplete(af.param2);
        return af.asyncResult();
    }

    @Override
    public CompletablePromise<Void> runAfterBoth(CompletionStage<?> completionStage, Runnable runnable) {
        return runAfterBothAsync(completionStage, runnable, AsyncProc.directExec);
    }

    @Override
    public CompletablePromise<Void> runAfterBothAsync(CompletionStage<?> completionStage, Runnable runnable) {
        return runAfterBothAsync(completionStage, runnable, null);
    }

    @Override
    public CompletablePromise<Void> runAfterBothAsync(CompletionStage<?> other, Runnable runnable, Executor executor) {
        AsyncBiFunction af = new AsyncBiFunction(runnable);
        af.setExecutor(executor);
        this.subscribe(af.param1);
        other.whenComplete(af.param2);
        return af.asyncResult();
    }

    @Override
    public <U> CompletablePromise<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> function) {
        return applyToEitherAsync(other, function, AsyncProc.directExec);
    }

    @Override
    public <U> CompletablePromise<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> function) {
        return applyToEitherAsync(other, function, null);
    }

    @Override
    public <U> CompletablePromise<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> function, Executor executor) {
        AsyncFunction af = new AsyncFunction(function);
        af.setExecutor(executor);
        this.subscribe(af);
        other.whenComplete(af);
        return af.asyncResult();
    }

    @Override
    public CompletablePromise<Void> acceptEither(CompletionStage<? extends T> completionStage, Consumer<? super T> consumer) {
        return null;
    }

    @Override
    public CompletablePromise<Void> acceptEitherAsync(CompletionStage<? extends T> completionStage, Consumer<? super T> consumer) {
        return null;
    }

    @Override
    public CompletablePromise<Void> acceptEitherAsync(CompletionStage<? extends T> completionStage, Consumer<? super T> consumer, Executor executor) {
        return null;
    }

    @Override
    public CompletablePromise<Void> runAfterEither(CompletionStage<?> completionStage, Runnable runnable) {
        return null;
    }

    @Override
    public CompletablePromise<Void> runAfterEitherAsync(CompletionStage<?> completionStage, Runnable runnable) {
        return null;
    }

    @Override
    public CompletablePromise<Void> runAfterEitherAsync(CompletionStage<?> completionStage, Runnable runnable, Executor executor) {
        return null;
    }

    @Override
    public <U> CompletablePromise<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> function) {
        return null;
    }

    @Override
    public <U> CompletablePromise<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> function) {
        return null;
    }

    @Override
    public <U> CompletablePromise<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> function, Executor executor) {
        return null;
    }

    @Override
    public CompletablePromise<T> exceptionally(Function<Throwable, ? extends T> function) {
        return null;
    }

    @Override
    public CompletablePromise<T> whenComplete(BiConsumer<? super T, ? super Throwable> biConsumer) {
        return whenCompleteAsync(biConsumer, AsyncProc.directExec);
    }

    @Override
    public CompletablePromise<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> biConsumer) {
        return whenCompleteAsync(biConsumer, null);
    }

    @Override
    public CompletablePromise<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> biConsumer, Executor executor) {
        BiconsumerSubscriber<T> subscriber = new BiconsumerSubscriber<T>(biConsumer, executor);
        this.whenComplete(subscriber.param);
        return null;
    }

    @Override
    public <U> CompletablePromise<U> handle(BiFunction<? super T, Throwable, ? extends U> biFunction) {
        return handleAsync(biFunction, AsyncProc.directExec);
    }

    @Override
    public <U> CompletablePromise<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> biFunction) {
        return handleAsync(biFunction, null);
    }

    @Override
    public <U> CompletablePromise<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> biFunction, Executor executor) {
        BiFunctionSubscriber<T, U> subscriber = new BiFunctionSubscriber(biFunction, executor);
        this.whenComplete(subscriber.param);
        return subscriber.asyncResult();
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        CompletableFuture<T> res = new CompletableFuture<>();
        this.subscribe(res);
        return res;
    }

    public static class BiconsumerSubscriber<T> extends AsyncProc<Void> {
        final ScalarInput<T> param = new ScalarInput<>(this);
        private final BiConsumer<? super T, ? super Throwable> biConsumer;

        public BiconsumerSubscriber(BiConsumer<? super T, ? super Throwable> biConsumer, Executor executor) {
            this.biConsumer = biConsumer;
            setExecutor(executor);
        }

        public void run() {
            biConsumer.accept(param.current(), param.completionException);
        }
    }

    public static class BiFunctionSubscriber<T, R> extends AsyncProc<R> {
        final ScalarInput<T> param = new ScalarInput<>(this);
        private final BiFunction<? super T, ? super Throwable, R> biFunction;

        public BiFunctionSubscriber(BiFunction<? super T, ? super Throwable, R> biFunction, Executor executor) {
            this.biFunction = biFunction;
            setExecutor(executor);
        }

        public void run() {
            try {
                result.onComplete(biFunction.apply(param.current(), param.completionException));
            } catch (Exception e) {
                result.onError(e);
            }
        }
    }
}
