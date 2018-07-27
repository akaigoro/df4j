package org.df4j.core.boundconnector.messagescalar;

import org.df4j.core.simplenode.messagescalar.AnyOf;
import org.df4j.core.simplenode.messagescalar.CompletedResult;
import org.df4j.core.simplenode.messagescalar.SubscriberCompletableFuture;
import org.df4j.core.simplenode.messagescalar.SubscriberPromise;
import org.df4j.core.tasknode.AsyncProc;
import org.df4j.core.tasknode.messagescalar.*;

import java.util.concurrent.*;
import java.util.function.*;

public interface AsyncResult<R> extends ScalarPublisher<R>, Future<R>, CompletionStage<R> {

    @Override
    default CompletableFuture<R> toCompletableFuture() {
        SubscriberCompletableFuture<R> result = new SubscriberCompletableFuture<R>();
        this.subscribe(result);
        return result;
    }

    /**
     * Returns a new SimpleAsyncResult that is asynchronously completed
     * by a task running in the {@link ForkJoinPool#commonPool()} with
     * the value obtained by calling the given Supplier.
     *
     * @param supplier a function returning the value to be used
     * to complete the returned SimpleAsyncResult
     * @param <U> the function's return type
     * @return the new SimpleAsyncResult
     */
    static <U> CompletablePromise<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, AsyncProc.asyncExec);
    }

    /**
     * Returns a new SimpleAsyncResult that is asynchronously completed
     * by a task running in the given executor with the value obtained
     * by calling the given Supplier.
     *
     * @param supplier a function returning the value to be used
     * to complete the returned SimpleAsyncResult
     * @param executor the executor to use for asynchronous execution
     * @param <U> the function's return type
     * @return the new SimpleAsyncResult
     */
    static <U> CompletablePromise<U> supplyAsync(Supplier<U> supplier,
                                                        Executor executor) {
        AsyncSupplier<U> asyncSupplier =  new AsyncSupplier<>(supplier);
        asyncSupplier.start(executor);
        return asyncSupplier.asyncResult();
    }

    /**
     * Returns a new SimpleAsyncResult that is asynchronously completed
     * by a task running in the {@link ForkJoinPool#commonPool()} after
     * it runs the given action.
     *
     * @param runnable the action to run before completing the
     * returned SimpleAsyncResult
     * @return the new SimpleAsyncResult
     */
    static CompletablePromise<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, AsyncProc.asyncExec);
    }

    /**
     * Returns a new SimpleAsyncResult that is asynchronously completed
     * by a task running in the given executor after it runs the given
     * action.
     *
     * @param runnable the action to run before completing the
     * returned SimpleAsyncResult
     * @param executor the executor to use for asynchronous execution
     * @return the new SimpleAsyncResult
     */
    static CompletablePromise<Void> runAsync(Runnable runnable,
                                                    Executor executor) {
        AsyncSupplier asyncTask = new AsyncSupplier<>(runnable);
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
    static <U> CompletedResult<U> completedFuture(U value) {
        CompletedResult<U> res = new CompletedResult<>(value);
        return res;
    }

    default <U> CompletablePromise<U> thenApply(Function<? super R,? extends U> fn) {
        return thenApplyAsync(fn, AsyncProc.syncExec);
    }

    default <U> CompletablePromise<U> thenApplyAsync(
            Function<? super R,? extends U> fn) {
        return thenApplyAsync(fn, AsyncProc.asyncExec);
    }

    default <U> CompletablePromise<U> thenApplyAsync(
            Function<? super R,? extends U> fn, Executor executor) {
        AsyncFunction<? super R,? extends U> asyncFunc =  new AsyncFunction<>(fn);
        this.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return (CompletablePromise<U>) asyncFunc.asyncResult();
    }


    default CompletablePromise<Void> thenAccept(Consumer<? super R> action) {
        return thenAcceptAsync(action, AsyncProc.syncExec);
    }

    default CompletablePromise<Void> thenAcceptAsync(Consumer<? super R> action) {
        return thenAcceptAsync(action, AsyncProc.asyncExec);
    }

    default CompletablePromise<Void> thenAcceptAsync(Consumer<? super R> action,
                                                    Executor executor) {
        AsyncFunction<R,Void> asyncConsumer =  new AsyncFunction<>(action);
        this.subscribe(asyncConsumer);
        asyncConsumer.start(executor);
        return asyncConsumer.asyncResult();
    }

    default CompletablePromise<Void> thenRun(Runnable action) {
        return thenRunAsync(action, AsyncProc.syncExec);
    }

    default CompletablePromise<Void> thenRunAsync(Runnable action) {
        return thenRunAsync(action, AsyncProc.asyncExec);
    }

    default CompletablePromise<Void> thenRunAsync(Runnable action,
                                                 Executor executor) {
        AsyncFunction<R, Void> asyncTask = new AsyncFunction<>(action);
        this.subscribe(asyncTask);
        asyncTask.start(executor);
        return asyncTask.asyncResult();
    }

    default <U,V> CompletablePromise<V> thenCombine(CompletionStage<? extends U> other,
                                                   BiFunction<? super R,? super U,? extends V> fn
    ) {
        return thenCombineAsync(other, fn, AsyncProc.syncExec);
    }

    default <U,V> CompletablePromise<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                        BiFunction<? super R,? super U,? extends V> fn
    ) {
        return thenCombineAsync(other, fn, AsyncProc.asyncExec);
    }

    default <U,V> CompletablePromise<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                        BiFunction<? super R, ? super U, ? extends V> fn,
                                                        Executor executor
    ){
        AsyncBiFunction<? super R, ? super U, V> asyncBiFunc = new AsyncBiFunction<>(fn);
        this.subscribe(asyncBiFunc.param1);
        other.whenComplete(asyncBiFunc.param2);
        asyncBiFunc.start(executor);
        CompletablePromise<V> result = asyncBiFunc.asyncResult();
        return result;
    }

    default <U> CompletablePromise<Void> thenAcceptBoth(
            CompletionStage<? extends U> other,
            BiConsumer<? super R, ? super U> action) {
        return thenAcceptBothAsync(other, action, AsyncProc.syncExec);
    }

    default <U> CompletablePromise<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other,
            BiConsumer<? super R, ? super U> action) {
        return thenAcceptBothAsync(other, action, AsyncProc.asyncExec);
    }

    default <U> CompletablePromise<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other,
            BiConsumer<? super R, ? super U> action, Executor executor) {
        AsyncBiFunction<? super R,? super U, Void> asyncBiConsumer = new AsyncBiFunction<>(action);
        this.subscribe(asyncBiConsumer.param1);
        ConstInput<? super U> handler = asyncBiConsumer.param2;
        BiConsumer<? super U, ? super Throwable> handler1 = handler;
        other.whenComplete(handler1);
        asyncBiConsumer.start(executor);
        return asyncBiConsumer.asyncResult();
    }

    default CompletablePromise<Void> runAfterBoth(CompletionStage<?> other,
                                                     Runnable action) {
        return runAfterBothAsync(other, action, AsyncProc.syncExec);
    }

    default CompletablePromise<Void> runAfterBothAsync(CompletionStage<?> other,
                                                          Runnable action) {
        return runAfterBothAsync(other, action, AsyncProc.asyncExec);
    }

    default CompletablePromise<Void> runAfterBothAsync(CompletionStage<?> other,
                                                          Runnable action,
                                                          Executor executor) {
        BiConsumer<R, Object> fn = (t,u)->action.run();
        AsyncBiFunction<R,Object, Void> asyncBiConsumer = new AsyncBiFunction<>(fn);
        this.subscribe(asyncBiConsumer.param1);
        other.whenComplete(asyncBiConsumer.param2);
        asyncBiConsumer.start(executor);
        return asyncBiConsumer.asyncResult();
    }

    default <U> CompletablePromise<U> applyToEither(
            CompletionStage<? extends R> other, Function<? super R, U> fn
    ) {
        return applyToEitherAsync(other, fn, AsyncProc.syncExec);
    }

    default <U> CompletablePromise<U> applyToEitherAsync(
            CompletionStage<? extends R> other, Function<? super R, U> fn) {
        return applyToEitherAsync(other, fn, AsyncProc.asyncExec);
    }

    default <U> CompletablePromise<U> applyToEitherAsync(
            CompletionStage<? extends R> other,
            Function<? super R, U> fn,
            Executor executor
    ) {
        AnyOf<R> either = new AnyOf<R>(this, other);
        AsyncFunction<? super R,U> asyncFunc =  new AsyncFunction<>(fn);
        either.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc.asyncResult();
    }

    default CompletablePromise<Void> acceptEither(
            CompletionStage<? extends R> other, Consumer<? super R> action) {
        return acceptEitherAsync(other, action, AsyncProc.syncExec);
    }

    default CompletablePromise<Void> acceptEitherAsync(
            CompletionStage<? extends R> other, Consumer<? super R> action) {
        return acceptEitherAsync(other, action, AsyncProc.asyncExec);
    }

    default CompletablePromise<Void> acceptEitherAsync(
            CompletionStage<? extends R> other, Consumer<? super R> action,
            Executor executor) {
        AnyOf<R> either = new AnyOf<>(this, other);
        AsyncFunction<? super R, Void> asyncFunc = new AsyncFunction<>(action);
        either.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc.asyncResult();
    }

    default CompletablePromise<Void> runAfterEither(CompletionStage<?> other,
                                                   Runnable action) {
        return runAfterEitherAsync(other, action, AsyncProc.syncExec);
    }

    default CompletablePromise<Void> runAfterEitherAsync(CompletionStage<?> other,
                                                        Runnable action) {
        return runAfterEitherAsync(other, action, AsyncProc.asyncExec);
    }

    default CompletablePromise<Void> runAfterEitherAsync(CompletionStage<?> other,
                                                        Runnable action,
                                                        Executor executor) {
        AnyOf<?> either = new AnyOf<>(this, other);
        AsyncFunction<Object, Void> asyncFunc = new AsyncFunction<>(action);
        either.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc.asyncResult();
    }

    default <U> CompletablePromise<U> thenCompose(
            Function<? super R, ? extends CompletionStage<U>> fn) {
        return thenComposeAsync(fn, AsyncProc.syncExec);
    }

    default <U> CompletablePromise<U> thenComposeAsync(
            Function<? super R, ? extends CompletionStage<U>> fn) {
        return thenComposeAsync(fn, AsyncProc.asyncExec);
    }

    default <U> CompletablePromise<U> thenComposeAsync(
            Function<? super R, ? extends CompletionStage<U>> fn,
            Executor executor) {
        throw new UnsupportedOperationException();
    }

    default CompletablePromise<R> whenComplete (
            BiConsumer<? super R, ? super Throwable> action
    ) {
        return whenCompleteAsync(action, AsyncProc.syncExec);
    }

    default CompletablePromise<R> whenCompleteAsync(
            BiConsumer<? super R, ? super Throwable> action) {
        return whenCompleteAsync(action, AsyncProc.asyncExec);
    }

    default CompletablePromise<R> whenCompleteAsync(
            BiConsumer<? super R, ? super Throwable> action,
            Executor executor) {
        BiFunction<? super R, ? super Throwable, ? extends R> action1
                = (arg, ex)->{action.accept(arg, ex);return arg;};
        AsyncHandler<R,R> asyncHandler = new AsyncHandler<R,R>(action1);
        this.subscribe(asyncHandler);
        asyncHandler.start(executor);
        return asyncHandler.asyncResult();
    }

    default <U> CompletablePromise<U> handle(
            BiFunction<? super R, Throwable, ? extends U> fn) {
        return handleAsync(fn, AsyncProc.syncExec);
    }

    default <U> CompletablePromise<U> handleAsync(
            BiFunction<? super R, Throwable, ? extends U> fn) {
        return handleAsync(fn, AsyncProc.asyncExec);
    }

    default <U> CompletablePromise<U> handleAsync(
            BiFunction<? super R, Throwable, ? extends U> fn, Executor executor) {
        AsyncHandler<R, U> handler = new AsyncHandler<>(fn);
        this.subscribe(handler);
        handler.start(executor);
        CompletablePromise<U> promise = handler.asyncResult();
        return promise;
    }

    /**
     * Returns a new AsyncResult that is completed when this
     * SimpleAsyncResult completes, with the result of the given
     * function of the exception triggering this AsyncResult's
     * completion when it completes exceptionally; otherwise, if this
     * AsyncResult completes normally, then the returned
     * AsyncResult also completes normally with the same value.
     * Note: More flexible versions of this functionality are
     * available using methods {@code whenComplete} and {@code handle}.
     *
     * @param fn the function to use to compute the value of the
     * returned AsyncResult if this AsyncResult completed
     * exceptionally
     * @return the new AsyncResult
     */
    default CompletablePromise<R> exceptionally(
            Function<Throwable, ? extends R> fn
    ) {
        BiFunction<? super R, Throwable, ? extends R> handler =
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
     * Returns a new AsyncResult that is completed when all of
     * the given CompletableFutures complete.  If any of the given
     * CompletableFutures complete exceptionally, then the returned
     * AsyncResult also does so, with a CompletionException
     * holding this exception as its cause.  Otherwise, the results,
     * if any, of the given CompletableFutures are not reflected in
     * the returned AsyncResult, but may be obtained by
     * inspecting them individually. If no CompletableFutures are
     * provided, returns a AsyncResult completed with the value
     * {@code null}.
     *
     * <p>Among the applications of this method is to await completion
     * of a set of independent CompletableFutures before continuing a
     * program, as in: {@code AsyncResult.allOf(c1, c2,
     * c3).join();}.
     *
     * @param cfs the CompletableFutures
     * @return a new AsyncResult that is completed when all of the
     * given CompletableFutures complete
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
     static CompletablePromise<Object[]> allOf(SubscriberPromise<?>... cfs) {
        AllOf allOf = new AllOf(cfs);
         CompletablePromise<Object[]> res = allOf.asyncResult();
        return res;
    }

    /**
     * Returns a new AsyncResult that is completed when any of
     * the given CompletableFutures complete, with the same result.
     * Otherwise, if it completed exceptionally, the returned
     * AsyncResult also does so, with a CompletionException
     * holding this exception as its cause.  If no CompletableFutures
     * are provided, returns an incomplete AsyncResult.
     *
     * @param cfs the CompletableFutures
     * @return a new AsyncResult that is completed with the
     * result or exception of any of the given CompletableFutures when
     * one completes
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
     static ScalarPublisher<Object> anyOf(SubscriberPromise<?>... cfs) {
        AnyOf<Object> either = new AnyOf<>(cfs);
        return either;
    }

}
