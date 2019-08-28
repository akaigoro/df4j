package org.df4j.core.asyncproc;

import org.df4j.core.protocols.Scalar;

import java.util.concurrent.*;
import java.util.function.*;

public interface Promise <R> extends Scalar.Publisher<R>, Future<R>, CompletionStage<R> {

    @Override
    default CompletableFuture<R> toCompletableFuture() {
        CompletableFuture<R> result = new CompletableFuture<R>();
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
    static <U> ScalarResult<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, AsyncProc.asyncExec);
    }

    /**
     * Returns a new CompletionStage that is asynchronously completed
     * by a task running in the given executor with the value obtained
     * by calling the given Supplier.
     *
     * @param supplier a function returning the value to be used
     * to complete the returned CompletionStage
     * @param executor the executor to use for asynchronous execution
     * @param <U> the function's return type
     * @return the new CompletionStage
     */
    static <U> ScalarResult<U> supplyAsync(Supplier<U> supplier,
                                                 Executor executor) {
        AsyncSupplier<U> asyncSupplier =  new AsyncSupplier<>(supplier);
        asyncSupplier.setExecutor(executor);
        asyncSupplier.fire();
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
    static ScalarResult<Void> runAsync(Runnable runnable) {
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
    static ScalarResult<Void> runAsync(Runnable runnable,
                                             Executor executor) {
        AsyncSupplier asyncTask = new AsyncSupplier<>(runnable);
        asyncTask.setExecutor(executor);
        asyncTask.fire();
        return asyncTask.asyncResult();
    }

    default <U> ScalarResult<U> thenApply(Function<? super R,? extends U> fn) {
        return thenApplyAsync(fn, AsyncProc.syncExec);
    }

    default <U> ScalarResult<U> thenApplyAsync(
            Function<? super R,? extends U> fn) {
        return thenApplyAsync(fn, AsyncProc.asyncExec);
    }

    default <U> ScalarResult<U> thenApplyAsync(
            Function<? super R,? extends U> fn, Executor executor) {
        AsyncFunction<? super R,? extends U> asyncFunc =  new AsyncFunction<>(fn);
        asyncFunc.setExecutor(executor);
        this.subscribe(asyncFunc);
        return (ScalarResult<U>) asyncFunc.asyncResult();
    }


    default Promise<Void> thenAccept(Consumer<? super R> action) {
        return thenAcceptAsync(action, AsyncProc.syncExec);
    }

    default Promise<Void> thenAcceptAsync(Consumer<? super R> action) {
        return thenAcceptAsync(action, AsyncProc.asyncExec);
    }

    default Promise<Void> thenAcceptAsync(Consumer<? super R> action,
                                                     Executor executor) {
        AsyncFunction<R,Void> asyncConsumer =  new AsyncFunction<>(action);
        asyncConsumer.setExecutor(executor);
        this.subscribe(asyncConsumer);
        return asyncConsumer.asyncResult();
    }

    default Promise<Void> thenRun(Runnable action) {
        return thenRunAsync(action, AsyncProc.syncExec);
    }

    default Promise<Void> thenRunAsync(Runnable action) {
        return thenRunAsync(action, AsyncProc.asyncExec);
    }

    default Promise<Void> thenRunAsync(Runnable action,
                                                  Executor executor) {
        AsyncFunction<R, Void> asyncTask = new AsyncFunction<>(action);
        asyncTask.setExecutor(executor);
        this.subscribe(asyncTask);
        return asyncTask.asyncResult();
    }

    default <U,V> ScalarResult<V> thenCombine(CompletionStage<? extends U> other,
                                                    BiFunction<? super R,? super U,? extends V> fn
    ) {
        return thenCombineAsync(other, fn, AsyncProc.syncExec);
    }

    default <U,V> ScalarResult<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                         BiFunction<? super R,? super U,? extends V> fn
    ) {
        return thenCombineAsync(other, fn, AsyncProc.asyncExec);
    }

    default <U,V> ScalarResult<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                         BiFunction<? super R, ? super U, ? extends V> fn,
                                                         Executor executor
    ){
        AsyncBiFunction<? super R, ? super U, V> asyncBiFunc = new AsyncBiFunction<>(fn);
        this.subscribe(asyncBiFunc.param1);
        asyncBiFunc.setExecutor(executor);
        other.whenComplete(asyncBiFunc.param2);
        return asyncBiFunc.asyncResult();
    }

    default <U> Promise<Void> thenAcceptBoth(
            CompletionStage<? extends U> other,
            BiConsumer<? super R, ? super U> action) {
        return thenAcceptBothAsync(other, action, AsyncProc.syncExec);
    }

    default <U> Promise<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other,
            BiConsumer<? super R, ? super U> action) {
        return thenAcceptBothAsync(other, action, AsyncProc.asyncExec);
    }

    default <U> Promise<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other,
            BiConsumer<? super R, ? super U> action, Executor executor) {
        AsyncBiFunction<? super R,? super U, Void> asyncBiFunction = new AsyncBiFunction<>(action);
        asyncBiFunction.setExecutor(executor);
        this.subscribe(asyncBiFunction.param1);
        other.whenComplete(asyncBiFunction.param2);
        return asyncBiFunction.asyncResult();
    }

    default Promise<Void> runAfterBoth(CompletionStage<?> other,
                                                  Runnable action) {
        return runAfterBothAsync(other, action, AsyncProc.syncExec);
    }

    default Promise<Void> runAfterBothAsync(CompletionStage<?> other,
                                                       Runnable action) {
        return runAfterBothAsync(other, action, AsyncProc.asyncExec);
    }

    default Promise<Void> runAfterBothAsync(CompletionStage<?> other,
                                                       Runnable action,
                                                       Executor executor) {
        BiConsumer<R, Object> fn = (t,u)->action.run();
        AsyncBiFunction<R,Object, Void> asyncBiConsumer = new AsyncBiFunction<>(fn);
        asyncBiConsumer.setExecutor(executor);
        this.subscribe(asyncBiConsumer.param1);
        other.whenComplete(asyncBiConsumer.param2);
        return asyncBiConsumer.asyncResult();
    }

    default <U> ScalarResult<U> applyToEither(
            CompletionStage<? extends R> other, Function<? super R, U> fn
    ) {
        return applyToEitherAsync(other, fn, AsyncProc.syncExec);
    }

    default <U> ScalarResult<U> applyToEitherAsync(
            CompletionStage<? extends R> other, Function<? super R, U> fn) {
        return applyToEitherAsync(other, fn, AsyncProc.asyncExec);
    }

    default <U> ScalarResult<U> applyToEitherAsync(
            CompletionStage<? extends R> other,
            Function<? super R, U> fn,
            Executor executor
    ) {
        AnyOf<R> either = new AnyOf<R>(this, other);
        AsyncFunction<? super R,U> asyncFunc =  new AsyncFunction<>(fn).setExecutor(executor);
        either.subscribe(asyncFunc);
        return asyncFunc.asyncResult();
    }

    default ScalarResult<Void> acceptEither(
            CompletionStage<? extends R> other, Consumer<? super R> action) {
        return acceptEitherAsync(other, action, AsyncProc.syncExec);
    }

    default ScalarResult<Void> acceptEitherAsync(
            CompletionStage<? extends R> other, Consumer<? super R> action) {
        return acceptEitherAsync(other, action, AsyncProc.asyncExec);
    }

    default ScalarResult<Void> acceptEitherAsync(
            CompletionStage<? extends R> other, Consumer<? super R> action,
            Executor executor) {
        AnyOf<R> either = new AnyOf<>(this, other);
        AsyncFunction<? super R, Void> asyncFunc = new AsyncFunction<>(action);
        asyncFunc.setExecutor(executor);
        either.subscribe(asyncFunc);
        return asyncFunc.asyncResult();
    }

    default ScalarResult<Void> runAfterEither(CompletionStage<?> other,
                                                    Runnable action) {
        return runAfterEitherAsync(other, action, AsyncProc.syncExec);
    }

    default ScalarResult<Void> runAfterEitherAsync(CompletionStage<?> other,
                                                         Runnable action) {
        return runAfterEitherAsync(other, action, AsyncProc.asyncExec);
    }

    default ScalarResult<Void> runAfterEitherAsync(CompletionStage<?> other,
                                                         Runnable action,
                                                         Executor executor) {
        AnyOf<?> either = new AnyOf<>(this, other);
        AsyncFunction<Object, Void> asyncFunc = new AsyncFunction<>(action);
        asyncFunc.setExecutor(executor);
        either.subscribe(asyncFunc);
        return asyncFunc.asyncResult();
    }

    default <U> ScalarResult<U> thenCompose(
            Function<? super R, ? extends CompletionStage<U>> fn) {
        return thenComposeAsync(fn, AsyncProc.syncExec);
    }

    default <U> ScalarResult<U> thenComposeAsync(
            Function<? super R, ? extends CompletionStage<U>> fn) {
        return thenComposeAsync(fn, AsyncProc.asyncExec);
    }

    default <U> ScalarResult<U> thenComposeAsync(
            Function<? super R, ? extends CompletionStage<U>> fn,
            Executor executor) {
        throw new UnsupportedOperationException();
    }

    default ScalarResult<R> whenComplete (
            BiConsumer<? super R, ? super Throwable> action
    ) {
        return whenCompleteAsync(action, AsyncProc.syncExec);
    }

    default ScalarResult<R> whenCompleteAsync(
            BiConsumer<? super R, ? super Throwable> action) {
        return whenCompleteAsync(action, AsyncProc.asyncExec);
    }

    default ScalarResult<R> whenCompleteAsync(
            BiConsumer<? super R, ? super Throwable> action,
            Executor executor) {
        BiFunction<? super R, ? super Throwable, ? extends R> action1
                = (arg, ex)->{action.accept(arg, ex);return arg;};
        AsyncProc<R> handler = new AsyncProc<R>() {
            protected final ScalarInput<R> argument = new ScalarInput<>(this);
            {
                setExecutor(executor);
                Promise.this.subscribe(argument);
            }
            @Override
            protected void run() {
                Throwable completionException = argument.completionException;
                if (completionException == null) {
                    try {
                        action1.apply(argument.current, null);
                        result.onSuccess(argument.current);
                    } catch (Exception e) {
                        result.onError(e);
                    }
                } else {
                    try {
                        action1.apply(null, completionException);
                    } catch (Exception e) {
                    }
                    result.onError(argument.completionException);
                }
            }
        };
        return handler.asyncResult();
    }

    default <U> ScalarResult<U> handle(
            BiFunction<? super R, Throwable, ? extends U> fn) {
        return handleAsync(fn, AsyncProc.syncExec);
    }

    default <U> ScalarResult<U> handleAsync(
            BiFunction<? super R, Throwable, ? extends U> fn) {
        return handleAsync(fn, AsyncProc.asyncExec);
    }

    default <U> ScalarResult<U> handleAsync(
            BiFunction<? super R, Throwable, ? extends U> fn, Executor executor) {
        AsyncProc<U> handler = new AsyncProc<U>() {
            protected final ScalarInput<R> argument = new ScalarInput<>(this);
            {
                setExecutor(executor);
                Promise.this.subscribe(argument);
            }
            @Override
            protected void run() {
                try {
                    U res = fn.apply(argument.current, argument.completionException);
                    result.onSuccess(res);
                } catch (Exception e) {
                    result.onError(e);
                }
            }
        };
        return handler.asyncResult();
    }

    /**
     * Returns a new Promise that is completed when this
     * Promise completes, with the result of the given
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
    default ScalarResult<R> exceptionally(
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
}
