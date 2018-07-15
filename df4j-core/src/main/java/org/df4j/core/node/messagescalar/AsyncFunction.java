package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ConstInput;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.util.invoker.*;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AsyncFunction<T, R> extends AsyncFunc<R> implements ScalarSubscriber<T> {
    protected final ConstInput<T> argument = new ConstInput<>(this);

    public AsyncFunction() { }

    public AsyncFunction(Function<T,R> fn) {
        super(new FunctionInvoker<>(fn));
    }

    public AsyncFunction(Consumer<? super T> action) {
        super(new ConsumerInvoker<>(action));
    }

    public AsyncFunction(Supplier<R> supplier) {
        super(new SupplierInvoker<>(supplier));
    }

    public AsyncFunction(Runnable action) {
        super(new RunnableInvoker<>(action));
    }

    @Override
    public void post(T message) {
        argument.post(message);
    }

    @Override
    public void postFailure(Throwable throwable) {
        argument.postFailure(throwable);
    }
}
