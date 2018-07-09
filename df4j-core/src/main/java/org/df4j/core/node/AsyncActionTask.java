package org.df4j.core.node;

import org.df4j.core.util.ActionCaller;

// TODO implement as a connector
public class AsyncActionTask<R> extends AsyncResult<R> {
    private ActionCaller<R> actionMethod;

    protected R runAction() throws Exception {
        controlLock.turnOff();
        if (actionMethod == null) {
            try {
                actionMethod = new ActionCaller(this, connectors.size());
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        Object[] args = consumeTokens();
        R  res = actionMethod.apply(args);
        return res;
    }

    @Override
    public void run() {
        try {
            R res = runAction();
            complete(res); // wrong
        } catch (Throwable e) {
            stop();
            completeExceptionally(e);
        }
    }
}
