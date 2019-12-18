package org.df4j.core.fancy;

import org.df4j.core.port.ScalarInput;
import org.df4j.core.protocol.Disposable;
import org.df4j.core.protocol.Scalar;
import org.df4j.core.fancy.invoker.ConsumerInvoker;
import org.df4j.core.fancy.invoker.FunctionInvoker;
import org.df4j.core.fancy.invoker.RunnableInvoker;

import java.util.function.Consumer;
import java.util.function.Function;

public class AsyncFunction<T, R> extends AsyncSupplier<R> implements Scalar.Subscriber<T> {
    protected final ScalarInput<T> argument = new ScalarInput<>(this);
    private Disposable s;

    public AsyncFunction() { }

    public AsyncFunction(Function<T,R> fn) {
        super(new FunctionInvoker<>(fn));
    }

    public AsyncFunction(Consumer<? super T> action) {
        super(new ConsumerInvoker<>(action));
    }

    public AsyncFunction(Runnable action) {
        super(new RunnableInvoker<>(action));
    }

    @Override
    public void onSubscribe(Disposable s) {
        this.s = s;
    }

    @Override
    public void onSuccess(T message) {
        argument.onSuccess(message);
    }

    @Override
    public void onError(Throwable throwable) {
        argument.onError(throwable);
    }
}
