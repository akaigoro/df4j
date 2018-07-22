package org.df4j.core.tasknode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.AsyncResult;
import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.simplenode.messagescalar.AnyOf;
import org.df4j.core.simplenode.messagescalar.CompletedResult;
import org.df4j.core.simplenode.messagescalar.SubscriberPromise;
import org.df4j.core.tasknode.AsyncProc;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.*;

/**
 *
 * @param <R> type of input parameter
 * @param <R> type of result
 */
public class CompletablePromise<R> implements AsyncResult<R> {

    /** place for demands */
    private Lobby<R> requests = new Lobby<>();
    protected boolean completed = false;
    protected R result = null;
    protected Throwable exception;
    protected final AsyncProc task;

    public CompletablePromise(AsyncProc task) {
        this.task = task;
    }

    public CompletablePromise() {
        this.task = null;
    }

    @Override
    public synchronized <S extends ScalarSubscriber<? super R>> S subscribe(S subscriber) {
        if (completed) {
            subscriber.post(result);
        } else if (exception != null) {
            subscriber.postFailure(exception);
        } else {
            requests.subscribe(subscriber);
        }
        return subscriber;
    }

    public synchronized boolean complete(R result) {
        if (isDone()) {
            return false;
        }
        this.result = result;
        this.completed = true;
        notifyAll();
        requests.post(result);
        requests = null;
        return true;
    }

    public synchronized boolean completeExceptionally(Throwable exception) {
        if (exception == null) {
            throw new IllegalArgumentException("AsyncResult::completeExceptionally(): argument may not be null");
        }
        if (isDone()) {
            return false;
        }
        this.exception = exception;
        requests.postFailure(exception);
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
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    public boolean isDone() {
        return completed || (exception != null);
    }

    @Override
    public synchronized R get() throws InterruptedException, ExecutionException {
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

    private R throwStoredException() throws ExecutionException {
        Throwable x=exception, cause;
        if (x instanceof CancellationException)
            throw (CancellationException)x;
        if ((x instanceof CompletionException) &&
                (cause = x.getCause()) != null)
            x = cause;
        throw new ExecutionException(x);
    }

    @Override
    public synchronized R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
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

    /* ------------- Just for fun:  public methods from j.u.c.CompletableFuture --------------
     * the return value is SimpleAsyncResult which is not a subscriber
     * Indeed, there is no sense to return a Subscriber
     */

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
    public static <U> CompletablePromise<U> supplyAsync(Supplier<U> supplier) {
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
    public static <U> CompletablePromise<U> supplyAsync(Supplier<U> supplier,
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
    public static CompletablePromise<Void> runAsync(Runnable runnable) {
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
    public static CompletablePromise<Void> runAsync(Runnable runnable,
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
    public static <U> CompletedResult<U> completedFuture(U value) {
        CompletedResult<U> res = new CompletedResult<>(value);
        return res;
    }

    public <U> CompletablePromise<U> thenApply(Function<? super R,? extends U> fn) {
        return thenApplyAsync(fn, AsyncProc.syncExec);
    }

    public <U> CompletablePromise<U> thenApplyAsync(
            Function<? super R,? extends U> fn) {
        return thenApplyAsync(fn, AsyncProc.asyncExec);
    }

    public <U> CompletablePromise<U> thenApplyAsync(
            Function<? super R,? extends U> fn, Executor executor) {
        AsyncFunction<? super R,? extends U> asyncFunc =  new AsyncFunction<>(fn);
        this.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return (CompletablePromise<U>) asyncFunc.asyncResult();
    }

    public CompletablePromise<Void> thenAccept(Consumer<? super R> action) {
        CompletablePromise<Void> result = new CompletablePromise<>(task);
        ScalarSubscriber<? super R> handler = new ScalarSubscriber<R>(){

            @Override
            public void post(R message) {
                action.accept(message);
                result.complete(null);
            }

            @Override
            public void postFailure(Throwable ex) {
                result.completeExceptionally(ex);
            }
        };
        this.subscribe(handler);
        return result;
    }

    public CompletablePromise<Void> thenAcceptAsync(Consumer<? super R> action) {
        return thenAcceptAsync(action, AsyncProc.asyncExec);
    }

    public CompletablePromise<Void> thenAcceptAsync(Consumer<? super R> action,
                                                    Executor executor) {
        AsyncFunction<R,Void> asyncConsumer =  new AsyncFunction<>(action);
        this.subscribe(asyncConsumer);
        asyncConsumer.start(executor);
        return asyncConsumer.asyncResult();
    }

    public CompletablePromise<Void> thenRun(Runnable action) {
        CompletablePromise<Void> result = new CompletablePromise<Void>(task);
        ScalarSubscriber<? super R> handler = new ScalarSubscriber<R>(){

            @Override
            public void post(R message) {
                action.run();
                result.complete(null);
            }

            @Override
            public void postFailure(Throwable ex) {
                result.completeExceptionally(ex);
            }
        };
        this.subscribe(handler);
        return result;
    }

    public CompletablePromise<Void> thenRunAsync(Runnable action) {
        return thenRunAsync(action, AsyncProc.asyncExec);
    }

    public CompletablePromise<Void> thenRunAsync(Runnable action,
                                                 Executor executor) {
        AsyncFunction<R, Void> asyncTask = new AsyncFunction<>(action);
        this.subscribe(asyncTask);
        asyncTask.start(executor);
        return asyncTask.asyncResult();
    }

    public <U,V> CompletablePromise<V> thenCombine(AsyncResult<? extends U> other,
                                                   BiFunction<? super R,? super U,? extends V> fn
    ) {
        return thenCombineAsync(other, fn, AsyncProc.syncExec);
    }

    public <U,V> CompletablePromise<V> thenCombineAsync(AsyncResult<? extends U> other,
                                                        BiFunction<? super R,? super U,? extends V> fn
    ) {
        return thenCombineAsync(other, fn, AsyncProc.asyncExec);
    }

    public <U,V> CompletablePromise<V> thenCombineAsync(AsyncResult<? extends U> other,
                                                        BiFunction<? super R, ? super U, ? extends V> fn,
                                                        Executor executor
    ){
        AsyncBiFunction<? super R, ? super U, V> asyncBiFunc = new AsyncBiFunction<>(fn);
        this.subscribe(asyncBiFunc.param1);
        other.subscribe(asyncBiFunc.param2);
        asyncBiFunc.start(executor);
        CompletablePromise<V> result = asyncBiFunc.asyncResult();
        return result;
    }

    public <U> CompletablePromise<Void> thenAcceptBoth(
            AsyncResult<? extends U> other,
            BiConsumer<? super R, ? super U> action) {
        return thenAcceptBothAsync(other, action, AsyncProc.syncExec);
    }

    public <U> CompletablePromise<Void> thenAcceptBothAsync(
            CompletablePromise<? extends U> other,
            BiConsumer<? super R, ? super U> action) {
        return thenAcceptBothAsync(other, action, AsyncProc.asyncExec);
    }

    public <U> CompletablePromise<Void> thenAcceptBothAsync(
            AsyncResult<? extends U> other,
            BiConsumer<? super R, ? super U> action, Executor executor) {
        AsyncBiFunction<? super R,? super U, Void> asyncBiConsumer = new AsyncBiFunction<>(action);
        this.subscribe(asyncBiConsumer.param1);
        other.subscribe(asyncBiConsumer.param2);
        asyncBiConsumer.start(executor);
        return asyncBiConsumer.asyncResult();
    }

    public <U> CompletablePromise<Void> runAfterBoth(AsyncResult<? extends  U> other,
                                                     Runnable action) {
        return runAfterBothAsync(other, action, AsyncProc.syncExec);
    }

    public <U> CompletablePromise<Void> runAfterBothAsync(AsyncResult<? extends  U> other,
                                                          Runnable action) {
        return runAfterBothAsync(other, action, AsyncProc.asyncExec);
    }

    public <U> CompletablePromise<Void> runAfterBothAsync(AsyncResult<? extends  U> other,
                                                          Runnable action,
                                                          Executor executor) {
        BiConsumer<R, U> fn = (t,u)->action.run();
        AsyncBiFunction<? super R,? super U, Void> asyncBiConsumer = new AsyncBiFunction<>(fn);
        this.subscribe(asyncBiConsumer.param1);
        other.subscribe(asyncBiConsumer.param2);
        asyncBiConsumer.start(executor);
        return asyncBiConsumer.asyncResult();
    }

    public <U> CompletablePromise<U> applyToEither(
            CompletablePromise<? extends R> other, Function<? super R, U> fn
    ) {
        return applyToEitherAsync(other, fn, AsyncProc.syncExec);
    }

    public <U> CompletablePromise<U> applyToEitherAsync(
            AsyncResult<? extends R> other, Function<? super R, U> fn) {
        return applyToEitherAsync(other, fn, AsyncProc.asyncExec);
    }

    public <U> CompletablePromise<U> applyToEitherAsync(
            AsyncResult<? extends R> other,
            Function<? super R, U> fn,
            Executor executor
    ) {
        AnyOf<R> either = new AnyOf<R>(this, other);
        AsyncFunction<? super R,U> asyncFunc =  new AsyncFunction<>(fn);
        either.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc.asyncResult();
    }

    public CompletablePromise<Void> acceptEither(
            AsyncResult<? extends R> other, Consumer<? super R> action) {
        return acceptEitherAsync(other, action, AsyncProc.syncExec);
    }

    public CompletablePromise<Void> acceptEitherAsync(
            AsyncResult<? extends R> other, Consumer<? super R> action) {
        return acceptEitherAsync(other, action, AsyncProc.asyncExec);
    }

    public CompletablePromise<Void> acceptEitherAsync(
            AsyncResult<? extends R> other, Consumer<? super R> action,
            Executor executor) {
        AnyOf<R> either = new AnyOf<>(this, other);
        AsyncFunction<? super R, Void> asyncFunc = new AsyncFunction<>(action);
        either.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc.asyncResult();
    }

    public CompletablePromise<Void> runAfterEither(AsyncResult<? extends R> other,
                                                   Runnable action) {
        return runAfterEitherAsync(other, action, AsyncProc.syncExec);
    }

    public CompletablePromise<Void> runAfterEitherAsync(AsyncResult<? extends R> other,
                                                        Runnable action) {
        return runAfterEitherAsync(other, action, AsyncProc.asyncExec);
    }

    public CompletablePromise<Void> runAfterEitherAsync(AsyncResult<? extends R> other,
                                                        Runnable action,
                                                        Executor executor) {
        AnyOf<R> either = new AnyOf<>(this, other);
        AsyncFunction<Object, Void> asyncFunc = new AsyncFunction<>(action);
        either.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return asyncFunc.asyncResult();
    }

    public <U> CompletablePromise<U> thenCompose(
            Function<? super R, ? extends AsyncResult<U>> fn) {
        return thenComposeAsync(fn, AsyncProc.syncExec);
    }

    public <U> CompletablePromise<U> thenComposeAsync(
            Function<? super R, ? extends CompletablePromise<U>> fn) {
        return thenComposeAsync(fn, AsyncProc.asyncExec);
    }

    public <U> CompletablePromise<U> thenComposeAsync(
            Function<? super R, ? extends AsyncResult<U>> fn,
            Executor executor) {
        throw new UnsupportedOperationException();
    }

    public CompletablePromise<R> whenComplete(
            BiConsumer<? super R, ? super Throwable> action
    ) {
        SubscriberPromise<R> result = new SubscriberPromise<R>(task) {

            @Override
            public boolean complete(R message) {
                action.accept(message, null);
                super.complete(message);
                return false;
            }

            @Override
            public boolean completeExceptionally(Throwable ex) {
                action.accept( null, ex);
                super.completeExceptionally(ex);
                return false;
            }
        };
        this.subscribe(result);
        return result;
    }

    public CompletablePromise<R> whenCompleteAsync(
            BiConsumer<? super R, ? super Throwable> action) {
        return whenCompleteAsync(action, AsyncProc.asyncExec);
    }

    public CompletablePromise<R> whenCompleteAsync(
            BiConsumer<? super R, ? super Throwable> action,
            Executor executor) {
        BiFunction<? super R, ? super Throwable, ? extends R> action1
                = (arg, ex)->{action.accept(arg, ex);return arg;};
        AsyncHandler<R,R> asyncHandler = new AsyncHandler<R,R>(action1);
        this.subscribe(asyncHandler);
        asyncHandler.start(executor);
        return asyncHandler.asyncResult();
    }

    public <U> CompletablePromise<U> handle(
            BiFunction<? super R, Throwable, ? extends U> fn) {
        return handleAsync(fn, AsyncProc.syncExec);
    }

    public <U> CompletablePromise<U> handleAsync(
            BiFunction<? super R, Throwable, ? extends U> fn) {
        return handleAsync(fn, AsyncProc.asyncExec);
    }

    public <U> CompletablePromise<U> handleAsync(
            BiFunction<? super R, Throwable, ? extends U> fn, Executor executor) {
        AsyncHandler<R, U> handler = new AsyncHandler<>(fn);
        this.subscribe(handler);
        handler.start(executor);
        CompletablePromise<U> promise = handler.asyncResult();
        return promise;
    }

    /**
     * Returns a new SimpleAsyncResult that is completed when this
     * SimpleAsyncResult completes, with the result of the given
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
    public CompletablePromise<R> exceptionally(
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
    public static CompletablePromise<Object[]> allOf(SubscriberPromise<?>... cfs) {
        AllOf allOf = new AllOf(cfs);
        CompletablePromise<Object[]> res = allOf.asyncResult();
        return res;
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
    public static ScalarPublisher<Object> anyOf(SubscriberPromise<?>... cfs) {
        AnyOf<Object> either = new AnyOf<>(cfs);
        return either;
    }

    class Lobby<R> {
        private ScalarSubscriber<? super R> request;
        private Queue<ScalarSubscriber<? super R>> requests;

        public synchronized  <S extends ScalarSubscriber<? super R>> void subscribe(S subscriber) {
            if (request == null) {
                request = subscriber;
            } else {
                if (requests == null) {
                    requests = new ArrayDeque<>();
                }
                requests.add(subscriber);
            }
        }

        private Executor getExecutor() {
            if (task != null) {
                Executor exec = task.getExecutor();
                if (exec != null) {
                    return exec;
                }
            }
            return ForkJoinPool.commonPool();
        }

        void post(R result) {
            if (request == null) {
                return;
            }
            request.post(result);
            if (requests == null) {
                return;
            }
            getExecutor().execute(new PostResult<R>(requests, result));
        }

        void postFailure(Throwable exception) {
            if (request == null) {
                return;
            }
            request.postFailure(exception);
            if (requests == null) {
                return;
            }
            getExecutor().execute(new PostFailure(requests, exception));
        }
    }

    static class PostResult<R> implements Runnable {
        private final Queue<ScalarSubscriber<? super R>> requests;
        private final R result;

        PostResult(Queue<ScalarSubscriber<? super R>> requests, R result) {
            this.requests = requests;
            this.result = result;
        }

        @Override
        public void run() {
            for (ScalarSubscriber<? super R> subscriber: requests) {
                subscriber.post(result);
            }
        }
    }

    static class PostFailure<R> implements Runnable {
        private final Queue<ScalarSubscriber<? super R>> requests;
        private final Throwable exception;

        PostFailure(Queue<ScalarSubscriber<? super R>> requests, Throwable exception) {
            this.requests = requests;
            this.exception = exception;
        }

        @Override
        public void run() {
            for (ScalarSubscriber<? super R> subscriber: requests) {
                subscriber.postFailure(exception);
            }
        }
    }
}
