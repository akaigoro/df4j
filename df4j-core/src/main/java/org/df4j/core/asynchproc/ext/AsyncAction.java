package org.df4j.core.asynchproc.ext;

import org.df4j.core.asynchproc.AsyncProc;
import org.df4j.core.util.ActionCaller;
import org.df4j.core.util.invoker.Invoker;

/**
 * this class action caller -- allows @Action annotation
 */
public class AsyncAction<R> extends AsyncProc<R> {

    protected Invoker actionCaller;

    public AsyncAction() {
    }

    public AsyncAction(Invoker actionCaller) {
        this.actionCaller = actionCaller;
    }

    public String toString() {
        return super.toString() + result.toString();
    }

    protected R callAction() throws Throwable {
        if (actionCaller == null) {
            actionCaller = ActionCaller.findAction(this, getParamCount());
        }
        Object[] args = collectTokens();
        R  res = (R) actionCaller.apply(args);
        return res;
    }

    protected void runAction() throws Throwable {
        callAction();
    }

    @Override
    public void run() {
        try {
            runAction();
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }
    }
}
