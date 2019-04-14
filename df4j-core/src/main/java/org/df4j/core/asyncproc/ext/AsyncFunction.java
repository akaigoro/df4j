package org.df4j.core.asyncproc.ext;

import org.df4j.core.asyncproc.ScalarInput;
import org.df4j.core.asyncproc.ScalarSubscriber;
import org.df4j.core.asyncproc.ScalarSubscriptionQueue;
import org.df4j.core.util.invoker.ConsumerInvoker;
import org.df4j.core.util.invoker.FunctionInvoker;
import org.df4j.core.util.invoker.RunnableInvoker;

import java.util.function.Consumer;
import java.util.function.Function;

public class AsyncFunction<T, R> extends AsyncSupplier<R> implements ScalarSubscriber<T> {
    protected final ScalarInput<T> argument = new ScalarInput<>(this);
    private ScalarSubscriptionQueue.ScalarSubscription s;

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
    public void onSubscribe(ScalarSubscriptionQueue.ScalarSubscription s) {
        this.s = s;
    }

    @Override
    public void onComplete(T message) {
        argument.onComplete(message);
    }

    @Override
    public void onError(Throwable throwable) {
        argument.onError(throwable);
    }
}
