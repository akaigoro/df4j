package org.df4j.core.connector.messagescalar;

import org.df4j.core.node.messagescalar.*;
import org.df4j.core.util.SameThreadExecutor;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * an unblocking single-shot output connector
 *
 * @param <T>
 */
public class CompletablePromise<T> implements ScalarSubscriber<T>,
        ScalarPublisher<T>, Future<T> {
    private static Executor syncExec = SameThreadExecutor.sameThreadExecutor;
    private static Executor asyncExec = ForkJoinPool.commonPool();

    protected SimpleSubscription subscription;
    /** place for demands */
    private Queue<ScalarSubscriber<? super T>> requests = new ArrayDeque<>();
    protected boolean cancelled = false;
    protected boolean completed = false;
    protected T result = null;
    protected Throwable exception;

    @Override
    public void post(T message) {
        complete(message);
    }

    @Override
    public void postFailure(Throwable ex) {
        completeExceptionally(ex);
    }

    @Override
    public void onSubscribe(SimpleSubscription subscription) {
        if (cancelled) {
            throw new IllegalStateException("cancelled already");
        }
        if (completed) {
            throw new IllegalStateException("completed already");
        }
        this.subscription = subscription;
    }


    @Override
    public synchronized <S extends ScalarSubscriber<? super T>> S subscribe(S subscriber) {
        if (completed) {
            subscriber.post(result);
        } else if (exception != null) {
            subscriber.postFailure(exception);
        } else {
            requests.add(subscriber);
        }
        return subscriber;
    }

    public synchronized boolean complete(T result) {
        if (isDone()) {
            return false;
        }
        this.result = result;
        this.completed = true;
        notifyAll();
        for (ScalarSubscriber<? super T> subscriber: requests) {
            subscriber.post(result);
        }
        requests = null;
        return true;
    }

    public synchronized boolean completeExceptionally(Throwable exception) {
        if (exception == null) {
            throw new IllegalArgumentException("CompletablePromise::completeExceptionally(): argument may not be null");
        }
        if (isDone()) {
            return false;
        }
        this.exception = exception;
        for (ScalarSubscriber<? super T> subscriber: requests) {
            subscriber.postFailure(exception);
        }
        requests = null;
        return true;
    }

    /**
     * wrong API design. Future is not a task.
     * @param mayInterruptIfRunning
     * @return
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        cancelled = true;
        if (subscription != null) {
            subscription.cancel();
        }
        return completeExceptionally(new CancellationException());
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isDone() {
        return completed || (exception != null);
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        for (;;) {
            if (result != null) {
                return result;
            } else if (exception != null) {
                return throwStoredException();
            } else {
                wait();
            }
        }
    }

    private T throwStoredException() throws ExecutionException {
        Throwable x=exception, cause;
        if (x instanceof CancellationException)
            throw (CancellationException)x;
        if ((x instanceof CompletionException) &&
                (cause = x.getCause()) != null)
            x = cause;
        throw new ExecutionException(x);
    }

    @Override
    public synchronized T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long end = System.currentTimeMillis()+ unit.toMillis(timeout);
        for (;;) {
            if (completed) {
                return result;
            } else if (exception != null) {
                throwStoredException();
            } else {
                long timeout1 = end - System.currentTimeMillis();
                if (timeout1 <= 0) {
                    throw new TimeoutException();
                }
                wait(timeout1);
            }
        }
    }

    /* ------------- Just for fun:  public methods from j.u.c.CompletableFuture -------------- */

    /**
     * Returns a new CompletablePromise that is asynchronously completed
     * by a task running in the {@link ForkJoinPool#commonPool()} with
     * the value obtained by calling the given Supplier.
     *
     * @param supplier a function returning the value to be used
     * to complete the returned CompletablePromise
     * @param <U> the function's return type
     * @return the new CompletablePromise
     */
    public static <U> CompletablePromise<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, asyncExec);
    }

    /**
     * Returns a new CompletablePromise that is asynchronously completed
     * by a task running in the given executor with the value obtained
     * by calling the given Supplier.
     *
     * @param supplier a function returning the value to be used
     * to complete the returned CompletablePromise
     * @param executor the executor to use for asynchronous execution
     * @param <U> the function's return type
     * @return the new CompletablePromise
     */
    public static <U> CompletablePromise<U> supplyAsync(Supplier<U> supplier,
                                                       Executor executor) {
        AsyncFunction<Void, U> asyncSupplier =  new AsyncFunction<>(supplier);
        asyncSupplier.start(executor);
        return asyncSupplier.asyncResult();
    }

    /**
     * Returns a new CompletablePromise that is asynchronously completed
     * by a task running in the {@link ForkJoinPool#commonPool()} after
     * it runs the given action.
     *
     * @param runnable the action to run before completing the
     * returned CompletablePromise
     * @return the new CompletablePromise
     */
    public static CompletablePromise<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, asyncExec);
    }

    /**
     * Returns a new CompletablePromise that is asynchronously completed
     * by a task running in the given executor after it runs the given
     * action.
     *
     * @param runnable the action to run before completing the
     * returned CompletablePromise
     * @param executor the executor to use for asynchronous execution
     * @return the new CompletablePromise
     */
    public static CompletablePromise<Void> runAsync(Runnable runnable,
                                                   Executor executor) {
        AsyncResult asyncTask = new AsyncResult<>(runnable);
        asyncTask.start(executor);
        return asyncTask.asyncResult();
    }

    /**
     * Returns a new CompletableFuture that is already completed with
     * the given value.
     *
     * @param value the value
     * @param <U> the type of the value
     * @return the completed CompletableFuture
     */
    public static <U> CompletablePromise<U> completedFuture(U value) {
        CompletablePromise<U> res = new CompletablePromise<>();
        res.complete(value);
        return res;
    }

    public <U> CompletablePromise<U> thenApply(Function<? super T,? extends U> fn) {
        return thenApplyAsync(fn, syncExec);
    }

    public <U> CompletablePromise<U> thenApplyAsync(
            Function<? super T,? extends U> fn) {
        return thenApplyAsync(fn, asyncExec);
    }

    public <U> CompletablePromise<U> thenApplyAsync(
            Function<? super T,? extends U> fn, Executor executor) {
        AsyncFunction<? super T,? extends U> asyncFunc =  new AsyncFunction<>(fn);
        this.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return (CompletablePromise<U>) asyncFunc.asyncResult();
    }

    public CompletablePromise<Void> thenAccept(Consumer<? super T> action) {
        return thenAcceptAsync(action, syncExec);
    }

    public CompletablePromise<Void> thenAcceptAsync(Consumer<? super T> action) {
        return thenAcceptAsync(action, asyncExec);
    }

    public CompletablePromise<Void> thenAcceptAsync(Consumer<? super T> action,
                                                   Executor executor) {
        AsyncFunction<T,Void> asyncConsumer =  new AsyncFunction<>(action);
        this.subscribe(asyncConsumer);
        asyncConsumer.start(executor);
        return asyncConsumer.asyncResult();
    }

    public CompletablePromise<Void> thenRun(Runnable action) {
        return thenRunAsync(action, syncExec);
    }

    public CompletablePromise<Void> thenRunAsync(Runnable action) {
        return thenRunAsync(action, asyncExec);
    }

    public CompletablePromise<Void> thenRunAsync(Runnable action,
                                                Executor executor) {
        AsyncFunction<T, Void> asyncTask = new AsyncFunction<>(action);
        this.subscribe(asyncTask);
        asyncTask.start(executor);
        return asyncTask.asyncResult();
    }

    public <U,V> CompletablePromise<V> thenCombine(CompletablePromise<? extends U> other,
                                                        BiFunction<? super T,? super U,? extends V> fn
    ) {
        return thenCombineAsync(other, fn, syncExec);
    }

    public <U,V> CompletablePromise<V> thenCombineAsync(CompletablePromise<? extends U> other,
                                                        BiFunction<? super T,? super U,? extends V> fn
    ) {
        return thenCombineAsync(other, fn, asyncExec);
    }

    public <U,V> CompletablePromise<V> thenCombineAsync(CompletablePromise<? extends U> other,
                                                       BiFunction<? super T,? super U,? extends V> fn,
                                                       Executor executor
    ){
        AsyncBiFunction<? super T, ? super U, V> asyncBiFunc = new AsyncBiFunction<>(fn);
        this.subscribe(asyncBiFunc.arg1);
        other.subscribe(asyncBiFunc.arg2);
        asyncBiFunc.start(executor);
        CompletablePromise<V> result = asyncBiFunc.asyncResult();
        return result;
    }

    public <U> CompletablePromise<Void> thenAcceptBoth(
            CompletablePromise<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return thenAcceptBothAsync(other, action, syncExec);
    }

    public <U> CompletablePromise<Void> thenAcceptBothAsync(
            CompletablePromise<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return thenAcceptBothAsync(other, action, asyncExec);
    }

    public <U> CompletablePromise<Void> thenAcceptBothAsync(
            CompletablePromise<? extends U> other,
            BiConsumer<? super T, ? super U> action, Executor executor) {
        AsyncBiFunction<? super T,? super U, Void> asyncBiConsumer = new AsyncBiFunction<>(action);
        this.subscribe(asyncBiConsumer.arg1);
        other.subscribe(asyncBiConsumer.arg2);
        asyncBiConsumer.start(executor);
        return asyncBiConsumer.asyncResult();
    }

    public <U> CompletablePromise<Void> runAfterBoth(CompletablePromise<? extends  U> other,
                                                    Runnable action) {
        return runAfterBothAsync(other, action, syncExec);
    }

    public <U> CompletablePromise<Void> runAfterBothAsync(CompletablePromise<? extends  U> other,
                                                         Runnable action) {
        return runAfterBothAsync(other, action, asyncExec);
    }

    public <U> CompletablePromise<Void> runAfterBothAsync(CompletablePromise<? extends  U> other,
                                                         Runnable action,
                                                         Executor executor) {
        BiConsumer<T, U> fn = (t,u)->action.run();
        AsyncBiFunction<? super T,? super U, Void> asyncBiConsumer = new AsyncBiFunction<>(fn);
        this.subscribe(asyncBiConsumer.arg1);
        other.subscribe(asyncBiConsumer.arg2);
        asyncBiConsumer.start(executor);
        return asyncBiConsumer.asyncResult();
    }

    public <U> CompletablePromise<U> applyToEither(
            CompletablePromise<? extends T> other, Function<? super T, U> fn) {
        return applyToEitherAsync(other, fn, syncExec);
    }

    public <U> CompletablePromise<U> applyToEitherAsync(
            CompletablePromise<? extends T> other, Function<? super T, U> fn) {
        return applyToEitherAsync(other, fn, asyncExec);
    }

    public <U> CompletablePromise<U> applyToEitherAsync(
            CompletablePromise<? extends T> other,
            Function<? super T, U> fn,
            Executor executor
    ) {
        AnyOf<T> either = new AnyOf<T>(this, other);
        AsyncFunction<? super T,U> asyncFunc =  new AsyncFunction<>(fn);
        either.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc.asyncResult();
    }

    public CompletablePromise<Void> acceptEither(
            CompletablePromise<? extends T> other, Consumer<? super T> action) {
        return acceptEitherAsync(other, action, syncExec);
    }

    public CompletablePromise<Void> acceptEitherAsync(
            CompletablePromise<? extends T> other, Consumer<? super T> action) {
        return acceptEitherAsync(other, action, asyncExec);
    }

    public CompletablePromise<Void> acceptEitherAsync(
            CompletablePromise<? extends T> other, Consumer<? super T> action,
            Executor executor) {
        AnyOf<T> either = new AnyOf<>(this, other);
        AsyncFunction<? super T, Void> asyncFunc = new AsyncFunction<>(action);
        either.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc.asyncResult();
    }

    public CompletablePromise<Void> runAfterEither(CompletablePromise<? extends T> other,
                                                  Runnable action) {
        return runAfterEitherAsync(other, action, syncExec);
    }

    public CompletablePromise<Void> runAfterEitherAsync(CompletablePromise<? extends T> other,
                                                       Runnable action) {
        return runAfterEitherAsync(other, action, asyncExec);
    }

    public CompletablePromise<Void> runAfterEitherAsync(CompletablePromise<? extends T> other,
                                                       Runnable action,
                                                       Executor executor) {
        AnyOf<T> either = new AnyOf<>(this, other);
        AsyncFunction<Object, Void> asyncFunc = new AsyncFunction<>(action);
        either.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc.asyncResult();
    }

    public <U> CompletablePromise<U> thenCompose(
            Function<? super T, ? extends CompletablePromise<U>> fn) {
        return thenComposeAsync(fn, syncExec);
    }

    public <U> CompletablePromise<U> thenComposeAsync(
            Function<? super T, ? extends CompletablePromise<U>> fn) {
        return thenComposeAsync(fn, asyncExec);
    }

    public <U> CompletablePromise<U> thenComposeAsync(
            Function<? super T, ? extends CompletablePromise<U>> fn,
            Executor executor) {
        throw new UnsupportedOperationException();
    }

    public CompletablePromise<T> whenComplete(
            BiConsumer<? super T, ? super Throwable> action) {
        return whenCompleteAsync(action, syncExec);
    }

    public CompletablePromise<T> whenCompleteAsync(
            BiConsumer<? super T, ? super Throwable> action) {
        return whenCompleteAsync(action, asyncExec);
    }

    public CompletablePromise<T> whenCompleteAsync(
            BiConsumer<? super T, ? super Throwable> action,
            Executor executor) {
        BiFunction<? super T, ? super Throwable, ? extends T> action1
                = (arg, ex)->{action.accept(arg, ex);return arg;};
        AsyncHandler<T,T> asyncHandler = new AsyncHandler<T,T>(action1);
        this.subscribe(asyncHandler);
        asyncHandler.start(executor);
        return asyncHandler.asyncResult();
    }

    public <U> CompletablePromise<U> handle(
            BiFunction<? super T, Throwable, ? extends U> fn) {
        return handleAsync(fn, syncExec);
    }

    public <U> CompletablePromise<U> handleAsync(
            BiFunction<? super T, Throwable, ? extends U> fn) {
        return handleAsync(fn, asyncExec);
    }

    public <U> CompletablePromise<U> handleAsync(
            BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        AsyncHandler<T, U> handler = new AsyncHandler<>(fn);
        this.subscribe(handler);
        handler.start(executor);
        CompletablePromise<U> promise = handler.asyncResult();
        return promise;
    }

    /**
     * Returns a new CompletablePromise that is completed when this
     * CompletablePromise completes, with the result of the given
     * function of the exception triggering this CompletablePromise's
     * completion when it completes exceptionally; otherwise, if this
     * CompletablePromise completes normally, then the returned
     * CompletablePromise also completes normally with the same value.
     * Note: More flexible versions of this functionality are
     * available using methods {@code whenComplete} and {@code handle}.
     *
     * @param fn the function to use to compute the value of the
     * returned CompletablePromise if this CompletablePromise completed
     * exceptionally
     * @return the new CompletablePromise
     */
    public CompletablePromise<T> exceptionally(
            Function<Throwable, ? extends T> fn) {
        BiFunction<? super T, Throwable, ? extends T> handler =
                (value, ex) -> {
                    if (ex != null) {
                        return fn.apply(ex);
                    } else {
                        return value;
                    }
                };
        return handleAsync(handler);
    }

    /* ------------- Arbitrary-arity constructions -------------- */

    /**
     * Returns a new CompletablePromise that is completed when all of
     * the given CompletableFutures complete.  If any of the given
     * CompletableFutures complete exceptionally, then the returned
     * CompletablePromise also does so, with a CompletionException
     * holding this exception as its cause.  Otherwise, the results,
     * if any, of the given CompletableFutures are not reflected in
     * the returned CompletablePromise, but may be obtained by
     * inspecting them individually. If no CompletableFutures are
     * provided, returns a CompletablePromise completed with the value
     * {@code null}.
     *
     * <p>Among the applications of this method is to await completion
     * of a set of independent CompletableFutures before continuing a
     * program, as in: {@code CompletablePromise.allOf(c1, c2,
     * c3).join();}.
     *
     * @param cfs the CompletableFutures
     * @return a new CompletablePromise that is completed when all of the
     * given CompletableFutures complete
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
    public static CompletablePromise<Void> allOf(CompletablePromise<?>... cfs) {
        AllOf allOf = new AllOf(cfs);
        CompletablePromise result = allOf.asyncResult();
        return result;
    }

    /**
     * Returns a new CompletablePromise that is completed when any of
     * the given CompletableFutures complete, with the same result.
     * Otherwise, if it completed exceptionally, the returned
     * CompletablePromise also does so, with a CompletionException
     * holding this exception as its cause.  If no CompletableFutures
     * are provided, returns an incomplete CompletablePromise.
     *
     * @param cfs the CompletableFutures
     * @return a new CompletablePromise that is completed with the
     * result or exception of any of the given CompletableFutures when
     * one completes
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
    public static ScalarPublisher<Object> anyOf(CompletablePromise<?>... cfs) {
        AnyOf<Object> either = new AnyOf<>(cfs);
        return either;
    }




    /**
     * Returns {@code true} if this AsyncResult completed
     * exceptionally, in any way. Possible causes include
     * cancellation, explicit invocation of {@code
     * completeExceptionally}, and abrupt termination of a
     * AsyncResult action.
     *
     * @return {@code true} if this AsyncResult completed
     * exceptionally
     */
    public boolean isCompletedExceptionally() {
        return exception != null;
    }


    /**
     * Returns the estimated number of CompletableFutures whose
     * completions are awaiting completion of this AsyncResult.
     * This method is designed for use in monitoring system state, not
     * for synchronization control.
     *
     * @return the number of dependent CompletableFutures
     */
    public synchronized int getNumberOfDependents() {
        if (requests == null) {
            return 0;
        }
        return requests.size();
    }

    /**
     * Forcibly sets or resets the value subsequently returned by
     * method {@link #get()} and related methods, whether or not
     * already completed. This method is designed for use only in
     * error recovery actions, and even in such situations may result
     * in ongoing dependent completions using established versus
     * overwritten outcomes.
     *
     * @param value the completion value
     */
    public void obtrudeValue(T value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Forcibly causes subsequent invocations of method {@link #get()}
     * and related methods to throw the given exception, whether or
     * not already completed. This method is designed for use only in
     * error recovery actions, and even in such situations may result
     * in ongoing dependent completions using established versus
     * overwritten outcomes.
     *
     * @param ex the exception
     * @throws NullPointerException if the exception is null
     */
    public void obtrudeException(Throwable ex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        int count = getNumberOfDependents();
        StringBuilder sb = new StringBuilder();
        if (completed) {
            sb.append("[Completed normally]");
        } else if (exception != null) {
            sb.append("[Completed exceptionally]");
        } else if (count == 0) {
            sb.append("[Not completed]");
        } else {
            sb.append("[Not completed, ");
            sb.append(count);
            sb.append(" dependents]");
        }
        return sb.toString();
    }
}
