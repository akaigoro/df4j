package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ScalarPublisher;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.*;
import java.util.function.*;

import static org.df4j.core.util.SameThreadExecutor.sameThreadExecutor;

/**
 * an equivalent to {@link java.util.concurrent.CompletableFuture}
 *
 * @param <R>
 */
public class AsyncResultFuture<R> extends AsyncResult<R> implements Future<R> {
    protected Future<R> future = asFuture();
    private boolean cancelled;

    public AsyncResultFuture() {
    }

    public AsyncResultFuture(Runnable runnable) {
        super(runnable);
    }

    @Override
    public boolean isDone() {
        return result.isDone();
    }

    @Override
    public boolean complete(R res) {
        return super.complete(res);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        return super.completeExceptionally(ex);
    }

    @Override
    public R get() throws InterruptedException, ExecutionException {
        if (cancelled) {
            throw new CancellationException();
        }
        return future.get();
    }

    @Override
    public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    /* ------------- Just for fun:  public methods from j.u.c.CompletableFuture -------------- */

    /**
     * Returns a new AsyncResultFuture that is asynchronously completed
     * by a task running in the {@link ForkJoinPool#commonPool()} with
     * the value obtained by calling the given Supplier.
     *
     * @param supplier a function returning the value to be used
     * to complete the returned AsyncResultFuture
     * @param <U> the function's return type
     * @return the new AsyncResultFuture
     */
    public static <U> AsyncResultFuture<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, null);
    }

    /**
     * Returns a new AsyncResultFuture that is asynchronously completed
     * by a task running in the given executor with the value obtained
     * by calling the given Supplier.
     *
     * @param supplier a function returning the value to be used
     * to complete the returned AsyncResultFuture
     * @param executor the executor to use for asynchronous execution
     * @param <U> the function's return type
     * @return the new AsyncResultFuture
     */
    public static <U> AsyncResultFuture<U> supplyAsync(Supplier<U> supplier,
                                                       Executor executor) {
        AsyncFunction<Void, U> asyncSupplier =  new AsyncFunction<>(supplier);
        asyncSupplier.start(executor);
        return asyncSupplier;
    }

    /**
     * Returns a new AsyncResultFuture that is asynchronously completed
     * by a task running in the {@link ForkJoinPool#commonPool()} after
     * it runs the given action.
     *
     * @param runnable the action to run before completing the
     * returned AsyncResultFuture
     * @return the new AsyncResultFuture
     */
    public static AsyncResultFuture<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, null);
    }

    /**
     * Returns a new AsyncResultFuture that is asynchronously completed
     * by a task running in the given executor after it runs the given
     * action.
     *
     * @param runnable the action to run before completing the
     * returned AsyncResultFuture
     * @param executor the executor to use for asynchronous execution
     * @return the new AsyncResultFuture
     */
    public static AsyncResultFuture<Void> runAsync(Runnable runnable,
                                                   Executor executor) {
        AsyncFunction asyncTask = new AsyncFunction(runnable);
        asyncTask.start(executor);
        return asyncTask;
    }

    public <U> AsyncResultFuture<U> thenApply(Function<? super R,? extends U> fn) {
        return thenApplyAsync(fn, sameThreadExecutor);
    }

    public <U> AsyncResultFuture<U> thenApplyAsync(
            Function<? super R,? extends U> fn) {
        return thenApplyAsync(fn, null);
    }

    public <U> AsyncResultFuture<U> thenApplyAsync(
            Function<? super R,? extends U> fn, Executor executor) {
        AsyncFunction<R,U> asyncFunc =  new AsyncFunction<>(fn);
        this.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc;
    }

    public AsyncResultFuture<Void> thenAccept(Consumer<? super R> action) {
        return thenAcceptAsync(action, sameThreadExecutor);
    }

    public AsyncResultFuture<Void> thenAcceptAsync(Consumer<? super R> action) {
        return thenAcceptAsync(action, null);
    }

    public AsyncResultFuture<Void> thenAcceptAsync(Consumer<? super R> action,
                                                   Executor executor) {
        AsyncFunction<R,Void> asyncConsumer =  new AsyncFunction<>(action);
        this.subscribe(asyncConsumer);
        asyncConsumer.start(executor);
        return asyncConsumer;
    }

    public AsyncResultFuture<Void> thenRun(Runnable action) {
        return thenRunAsync(action, sameThreadExecutor);
    }

    public AsyncResultFuture<Void> thenRunAsync(Runnable action) {
        return thenRunAsync(action, null);
    }

    public AsyncResultFuture<Void> thenRunAsync(Runnable action,
                                                Executor executor) {
        AsyncFunction<R, Void> asyncTask = new AsyncFunction<>(action);
        this.subscribe(asyncTask);
        asyncTask.start(executor);
        return asyncTask;
    }

    public <U, T> AsyncResultFuture<T> thenCombine(
            AsyncResultFuture<? extends U> other,
            BiFunction<? super R,? super U,? extends T> fn) {
        return thenCombineAsync(other, fn, sameThreadExecutor);
    }

    public <U, T> AsyncResultFuture<T> thenCombineAsync(
            AsyncResultFuture<? extends U> other,
            BiFunction<? super R,? super U,? extends T> fn) {
        return thenCombineAsync(other, fn, null);
    }

    public <U, T> AsyncResultFuture<T> thenCombineAsync(AsyncResultFuture<? extends U> other,
                                                        BiFunction<? super R,? super U,? extends T> fn,
                                                        Executor executor
    ){
        AsyncBiFunction<R,U, T> asyncBiFunc = new AsyncBiFunction<>(fn);
        this.subscribe(asyncBiFunc.arg1);
        other.subscribe(asyncBiFunc.arg2);
        asyncBiFunc.start(executor);
        return asyncBiFunc;
    }

    public <U> AsyncResultFuture<Void> thenAcceptBoth(
            AsyncResultFuture<? extends U> other,
            BiConsumer<? super R, ? super U> action) {
        return thenAcceptBothAsync(other, action, sameThreadExecutor);
    }

    public <U> AsyncResultFuture<Void> thenAcceptBothAsync(
            AsyncResultFuture<? extends U> other,
            BiConsumer<? super R, ? super U> action) {
        return thenAcceptBothAsync(other, action, null);
    }

    public <U> AsyncResultFuture<Void> thenAcceptBothAsync(
            AsyncResultFuture<? extends U> other,
            BiConsumer<? super R, ? super U> action, Executor executor) {
        AsyncBiFunction<? super R,? super U, Void> asyncBiConsumer = new AsyncBiFunction<>(action);
        this.subscribe(asyncBiConsumer.arg1);
        other.subscribe(asyncBiConsumer.arg2);
        asyncBiConsumer.start(executor);
        return asyncBiConsumer;
    }

    public <U> AsyncResultFuture<Void> runAfterBoth(AsyncResultFuture<? extends  U> other,
                                                    Runnable action) {
        return runAfterBothAsync(other, action, sameThreadExecutor);
    }

    public <U> AsyncResultFuture<Void> runAfterBothAsync(AsyncResultFuture<? extends  U> other,
                                                         Runnable action) {
        return runAfterBothAsync(other, action, null);
    }

    public <U> AsyncResultFuture<Void> runAfterBothAsync(AsyncResultFuture<? extends  U> other,
                                                         Runnable action,
                                                         Executor executor) {
        AsyncBiFunction<? super R,? super U, Void> asyncBiConsumer = new AsyncBiFunction<>(action);
        this.subscribe(asyncBiConsumer.arg1);
        other.subscribe(asyncBiConsumer.arg2);
        asyncBiConsumer.start(executor);
        return asyncBiConsumer;
    }

    public <U> AsyncResultFuture<U> applyToEither(
            AsyncResultFuture<? extends R> other, Function<? super R, U> fn) {
        return applyToEitherAsync(other, fn, sameThreadExecutor);
    }

    public <U> AsyncResultFuture<U> applyToEitherAsync(
            AsyncResultFuture<? extends R> other, Function<? super R, U> fn) {
        return applyToEitherAsync(other, fn, null);
    }

    public <U> AsyncResultFuture<U> applyToEitherAsync(
            AsyncResultFuture<? extends R> other, Function<? super R, U> fn,
            Executor executor) {
        AnyOf<R> either = new AnyOf<>(this, other);
        AsyncFunction<R,U> asyncFunc =  new AsyncFunction<>(fn);
        either.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc;
    }

    public AsyncResultFuture<Void> acceptEither(
            AsyncResultFuture<? extends R> other, Consumer<? super R> action) {
        return acceptEitherAsync(other, action, sameThreadExecutor);
    }

    public AsyncResultFuture<Void> acceptEitherAsync(
            AsyncResultFuture<? extends R> other, Consumer<? super R> action) {
        return acceptEitherAsync(other, action, null);
    }

    public AsyncResultFuture<Void> acceptEitherAsync(
            AsyncResultFuture<? extends R> other, Consumer<? super R> action,
            Executor executor) {
        AnyOf<R> either = new AnyOf<>(this, other);
        AsyncFunction<? super R, Void> asyncFunc = new AsyncFunction<>(action);
        either.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc;
    }

    public AsyncResultFuture<Void> runAfterEither(AsyncResultFuture<?> other,
                                                  Runnable action) {
        return runAfterEitherAsync(other, action, sameThreadExecutor);
    }

    public AsyncResultFuture<Void> runAfterEitherAsync(AsyncResultFuture<?> other,
                                                       Runnable action) {
        return runAfterEitherAsync(other, action, null);
    }

    public AsyncResultFuture<Void> runAfterEitherAsync(AsyncResultFuture<?> other,
                                                       Runnable action,
                                                       Executor executor) {
        AnyOf<R> either = new AnyOf<>(this, other);
        AsyncFunction<Object, Void> asyncFunc = new AsyncFunction<>(action);
        either.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc;
    }

    public <U> AsyncResultFuture<U> thenCompose(
            Function<? super R, ? extends AsyncResultFuture<U>> fn) {
        return thenComposeAsync(fn, sameThreadExecutor);
    }

    public <U> AsyncResultFuture<U> thenComposeAsync(
            Function<? super R, ? extends AsyncResultFuture<U>> fn) {
        return thenComposeAsync(fn, null);
    }

    public <U> AsyncResultFuture<U> thenComposeAsync(
            Function<? super R, ? extends AsyncResultFuture<U>> fn,
            Executor executor) {
        throw new NotImplementedException();
    }

    public AsyncResultFuture<R> whenComplete(
            BiConsumer<? super R, ? super Throwable> action) {
        return whenCompleteAsync(action, sameThreadExecutor);
    }

    public AsyncResultFuture<R> whenCompleteAsync(
            BiConsumer<? super R, ? super Throwable> action) {
        return whenCompleteAsync(action, null);
    }

    public AsyncResultFuture<R> whenCompleteAsync(
            BiConsumer<? super R, ? super Throwable> action, Executor executor) {
        AsyncHandler<? super R, R> asyncHandler = new AsyncHandler<>(action);
        this.subscribe(asyncHandler);
        asyncHandler.start(executor);
        return asyncHandler;
    }

    public <U> AsyncResultFuture<U> handle(
            BiFunction<? super R, Throwable, ? extends U> fn) {
        return handleAsync(fn, sameThreadExecutor);
    }

    public <U> AsyncResultFuture<U> handleAsync(
            BiFunction<? super R, Throwable, ? extends U> fn) {
        return handleAsync(fn, null);
    }

    public <U> AsyncResultFuture<U> handleAsync(
            BiFunction<? super R, Throwable, ? extends U> handler, Executor executor) {
        AsyncHandler<R,U> asyncHandler =  new AsyncHandler<>(handler);
        this.subscribe(asyncHandler);
        asyncHandler.start(executor);
        return asyncHandler;
    }

    /**
     * Returns a new AsyncResultFuture that is completed when this
     * AsyncResultFuture completes, with the result of the given
     * function of the exception triggering this AsyncResultFuture's
     * completion when it completes exceptionally; otherwise, if this
     * AsyncResultFuture completes normally, then the returned
     * AsyncResultFuture also completes normally with the same value.
     * Note: More flexible versions of this functionality are
     * available using methods {@code whenComplete} and {@code handle}.
     *
     * @param fn the function to use to compute the value of the
     * returned AsyncResultFuture if this AsyncResultFuture completed
     * exceptionally
     * @return the new AsyncResultFuture
     */
    public AsyncResultFuture<R> exceptionally(
            Function<Throwable, ? extends R> fn) {
        BiFunction<? super R, Throwable, ? extends R> handler =
                (value, ex) -> {
                    if (ex != null) {
                        return fn.apply(ex);
                    } else {
                        return value;
                    }
                };
        return handleAsync(handler, executor);
    }

    /* ------------- Arbitrary-arity constructions -------------- */

    /**
     * Returns a new AsyncResultFuture that is completed when all of
     * the given CompletableFutures complete.  If any of the given
     * CompletableFutures complete exceptionally, then the returned
     * AsyncResultFuture also does so, with a CompletionException
     * holding this exception as its cause.  Otherwise, the results,
     * if any, of the given CompletableFutures are not reflected in
     * the returned AsyncResultFuture, but may be obtained by
     * inspecting them individually. If no CompletableFutures are
     * provided, returns a AsyncResultFuture completed with the value
     * {@code null}.
     *
     * <p>Among the applications of this method is to await completion
     * of a set of independent CompletableFutures before continuing a
     * program, as in: {@code AsyncResultFuture.allOf(c1, c2,
     * c3).join();}.
     *
     * @param cfs the CompletableFutures
     * @return a new AsyncResultFuture that is completed when all of the
     * given CompletableFutures complete
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
    public static AsyncResultFuture<Void> allOf(AsyncResultFuture<?>... cfs) {
        AllOf allOf = new AllOf(cfs);
        return allOf;
    }

    /**
     * Returns a new AsyncResultFuture that is completed when any of
     * the given CompletableFutures complete, with the same result.
     * Otherwise, if it completed exceptionally, the returned
     * AsyncResultFuture also does so, with a CompletionException
     * holding this exception as its cause.  If no CompletableFutures
     * are provided, returns an incomplete AsyncResultFuture.
     *
     * @param cfs the CompletableFutures
     * @return a new AsyncResultFuture that is completed with the
     * result or exception of any of the given CompletableFutures when
     * one completes
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
    public static ScalarPublisher<Object> anyOf(AsyncResultFuture<?>... cfs) {
        AnyOf<Object> either = new AnyOf<>(cfs);
        return either;
    }




    /* ------------- Control and status methods -------------- */

    /**
     * If not already completed, completes this AsyncResultFuture with
     * a {@link CancellationException}. Dependent CompletableFutures
     * that have not already completed will also complete
     * exceptionally, with a {@link CompletionException} caused by
     * this {@code CancellationException}.
     *
     * @param mayInterruptIfRunning this value has no effect in this
     * implementation because interrupts are not used to control
     * processing.
     *
     * @return {@code true} if this task is now cancelled
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        cancelled = true;
        return false;
    }

    /**
     * Returns {@code true} if this AsyncResultFuture was cancelled
     * before it completed normally.
     *
     * @return {@code true} if this AsyncResultFuture was cancelled
     * before it completed normally
     */
    public boolean isCancelled() {
        return  cancelled;
    }

}
