package org.df4j.core.tasknode.messagescalar;

import org.reactivestreams.Subscriber;
import org.df4j.core.util.invoker.ConsumerInvoker;
import org.df4j.core.util.invoker.FunctionInvoker;
import org.df4j.core.util.invoker.RunnableInvoker;
import org.reactivestreams.Subscription;

import java.util.function.Consumer;
import java.util.function.Function;

public class AsyncFunction<T, R> extends AsyncSupplier<R> implements Subscriber<T> {
    protected final ConstInput<T> argument = new ConstInput<>();
    private Subscription s;

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
    public void onSubscribe(Subscription s) {
        this.s = s;
    }

    @Override
    public void onNext(T message) {
        argument.onNext(message);
    }

    @Override
    public void onError(Throwable throwable) {
        argument.onError(throwable);
    }

    @Override
    public void onComplete() {
        onNext(null);
    }
}
