package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ConstInput;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.node.Action;
import org.df4j.core.node.AsyncResult;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AsyncFunction<T, R> extends AsyncResult<R> implements ScalarSubscriber<T> {
    protected final ConstInput<T> argument = new ConstInput<>(this);
    @Action
    protected Function<? super T,? extends R> fn;

    public AsyncFunction() {
    }

    public AsyncFunction(Function<? super T,? extends R> fn) {
        this.fn = fn;
    }

    public AsyncFunction(Consumer<? super T> action) {
        this.fn = arg -> {action.accept(arg); return null;};
    }

    public AsyncFunction(Supplier<R> supplier) {
        this.fn = arg -> supplier.get();
    }

    public AsyncFunction(Runnable action) {
        this.fn = arg -> {action.run(); return null;};
    }

    @Override
    public void post(T message) {
        argument.post(message);
    }

    @Override
    public void postFailure(Throwable throwable) {
        argument.postFailure(throwable);
    }
/*
    public void run() {
        runFunction(argument.next());
    }

    protected void runFunction(T argument) {
        try {
            R res = fn.apply(argument);
            result.complete(res);
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }
    }
*/
}
