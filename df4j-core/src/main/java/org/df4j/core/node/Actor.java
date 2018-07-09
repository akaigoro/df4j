package org.df4j.core.node;

/**
 * Actor is a reusable AsyncTask: after execution, it executes again as soon as new array of arguments is ready.
 */
public class Actor<R> extends AsyncActionTask<R> {
    @Override
    public void run() {
        try {
            R res = runAction();
  //          complete(res);
            start();
        } catch (Throwable e) {
            stop();
            completeExceptionally(e);
        }
    }

}
