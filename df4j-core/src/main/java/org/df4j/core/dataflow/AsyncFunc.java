package org.df4j.core.dataflow;

import org.df4j.core.communicator.ScalarResultTrait;

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
