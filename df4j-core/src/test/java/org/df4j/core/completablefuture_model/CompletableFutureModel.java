package org.df4j.core.completablefuture_model;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * the code to the article /df4j/df4j-core/UnderTheHood.md
 *
 * @param <T>
 */
public class CompletableFutureModel<T> extends CompletableFuture<T> {

    static class CompletableFutureForSupplyAsync<U> extends CompletableFuture<U> implements Runnable {
        Supplier<U> supplier;

        public CompletableFutureForSupplyAsync(Supplier<U> supplier) {
            this.supplier = supplier;
        }

        public void run() {
            try {
                U result = supplier.get();
                super.complete(result);
            } catch (Throwable e) {
                super.completeExceptionally(e);
            }
        }
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier,
                                                       Executor executor
    ) {
        CompletableFutureForSupplyAsync<U> task = new CompletableFutureForSupplyAsync<>(supplier);
        executor.execute(task);
        return task;
    }


    static class CompletableFutureForApplyAsync<T, U> extends CompletableFuture<U>
            implements Runnable, BiConsumer<T,Throwable>
    {
        Function<? super T,? extends U> fn;
        Executor executor;
        T argument;
        Throwable throwable;

        public CompletableFutureForApplyAsync(Function<? super T,? extends U> fn, Executor executor) {
            this.fn = fn;
            this.executor = executor;
        }

        @Override
        public void accept(T argument, Throwable throwable) {
            if (throwable != null) {
                this.throwable = throwable;
            } else {
                this.argument = argument;
            }
            executor.execute(this);
        }

        public void run() {
            if (throwable == null) {
                try {
                    U result = fn.apply(argument);
                    super.complete(result);
                } catch (Throwable e) {
                    super.completeExceptionally(e);
                }
            } else {
                super.completeExceptionally(throwable);
            }
        }
    }

    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T,? extends U> fn,
                                                   Executor executor) {
        CompletableFutureForApplyAsync<T,U> task = new CompletableFutureForApplyAsync<>(fn, executor);
        this.whenComplete(task);
        return task;
    }

    static class CompletableFutureForCombineAsync<T, U, V> extends CompletableFuture<V>
            implements Runnable
    {
        BiFunction<? super T,? super U,? extends V> fn;
        Executor executor;
        T arg1;
        U arg2;
        Throwable throwable;

        public CompletableFutureForCombineAsync(CompletionStage<? extends U> other,
                                                BiFunction<? super T,? super U,? extends V> fn,
                                                Executor executor) {
            this.fn = fn;
            this.executor = executor;
        }

        public synchronized void accept1(T argument, Throwable throwable) {
            if (throwable != null) {
                this.throwable = throwable;
                executor.execute(this);
            } else {
                this.arg1 = argument;
                if (arg2 != null) {
                    executor.execute(this);
                }
            }
        }

        public void accept2(U argument, Throwable throwable) {
            if (throwable != null) {
                this.throwable = throwable;
                executor.execute(this);
            } else {
                this.arg2 = argument;
                if (arg1 != null) {
                    executor.execute(this);
                }
            }
        }

        public void run() {
            if (throwable == null) {
                try {
                    V result = fn.apply(arg1, arg2);
                    super.complete(result);
                } catch (Throwable e) {
                    super.completeExceptionally(e);
                }
            } else {
                super.completeExceptionally(throwable);
            }
        }
    }

    public <U,V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                       BiFunction<? super T,? super U,? extends V> fn,
                                                       Executor executor) {
        CompletableFutureForCombineAsync<T,U, V> task = new CompletableFutureForCombineAsync<>(other, fn, executor);
        this.whenComplete(task::accept1);
        other.whenComplete(task::accept2);
        return task;
    }
}
