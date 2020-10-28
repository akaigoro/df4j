package org.df4j.core.actor;

import org.df4j.core.connector.Completion;
import org.df4j.core.connector.ScalarResult;
import org.df4j.protocol.Scalar;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AsyncFunc<R> extends AsyncProc implements Scalar.Source<R>, Scalar.Publisher<R> {

    public AsyncFunc(ActorGroup parent) {
        super(parent);
    }

    public AsyncFunc() {
        super();
    }

    @Override
    protected @NotNull Completion createCompletion() {
        return new ScalarResult<R>();
    }

    protected ScalarResult<R> getResult() {
        return (ScalarResult<R>) completion;
    }

    @Override
    public R get() throws InterruptedException {
        return getResult().get();
    }

    public R get(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        return getResult().get(timeout, unit);
    }

    @Override
    public void subscribe(Scalar.Subscriber<? super R> s) {
        getResult().subscribe(s);
    }

    protected abstract R callAction() throws Throwable;

    @Override
    protected void runAction() throws Throwable {
        R result = callAction();
        getResult().setResult(result);
    }
}
