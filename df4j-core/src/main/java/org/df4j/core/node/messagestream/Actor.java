package org.df4j.core.node.messagestream;

import org.df4j.core.node.AsyncTask;

/**
 * Actor is a reusable AsyncTask: after execution, it executes again as soon as new array of arguments is ready.
 */
public class Actor<R> extends AsyncTask<R> {
    @Override
    public void run() {
        try {
            R res = runAction();
            start(); // restart execution
        } catch (Throwable e) {
            stop();
        }
    }

}
