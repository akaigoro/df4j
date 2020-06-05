package org.df4j.core.dataflow;

import org.df4j.core.communicator.ScalarResultTrait;
import org.df4j.protocol.Completable;
import org.df4j.protocol.Scalar;
import org.df4j.protocol.SimpleSubscription;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AsyncFunc<R> extends AsyncProc implements ScalarResultTrait<R> {
    private R result;

    public AsyncFunc(Dataflow parent) {
        super(parent);
    }

    public AsyncFunc() {
        super();
    }

    @Override
    public R getResult() {
        return result;
    }

    @Override
    public void setResult(R result) {
        this.result = result;
    }

    protected abstract R callAction() throws Throwable;

    @Override
    protected void runAction() throws Throwable {
        result = callAction();
    }
}
