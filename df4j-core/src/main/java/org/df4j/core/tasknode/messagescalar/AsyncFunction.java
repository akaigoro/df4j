package org.df4j.core.tasknode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ConstInput;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.util.invoker.ConsumerInvoker;
import org.df4j.core.util.invoker.FunctionInvoker;
import org.df4j.core.util.invoker.RunnableInvoker;

import java.util.function.Consumer;
import java.util.function.Function;

public class AsyncFunction<T, R> extends AsyncSupplier<R> implements ScalarSubscriber<T> {
    protected final ConstInput<T> argument = new ConstInput<>(this);

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
    public boolean complete(T message) {
        return argument.complete(message);
    }

    @Override
    public boolean completeExceptionally(Throwable throwable) {
        return argument.completeExceptionally(throwable);
    }
}
