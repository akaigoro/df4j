package main.java.org.df4j.fancy;

import main.java.org.df4j.fancy.invoker.Invoker;
import org.df4j.core.actor.AsyncProc;

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
        Object[] args = collectArgs();
        R  res = (R) actionCaller.apply(args);
        return res;
    }

    protected void runAction() throws Throwable {
        callAction();
    }

    @Override
    protected void run() {
        try {
            runAction();
        } catch (Throwable e) {
            result.onError(e);
        }
    }
}
