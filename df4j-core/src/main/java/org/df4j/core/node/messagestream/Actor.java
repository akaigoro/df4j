package org.df4j.core.node.messagestream;

import org.df4j.core.node.AsyncTask;

/**
 * Actor is a reusable AsyncTask: after execution, it executes again as soon as new array of arguments is ready.
 */
public class Actor extends AsyncTask<Void> {
    @Override
    public void run() {
        try {
            controlLock.turnOff();
            runAction();
            start(); // restart execution
        } catch (Throwable e) {
            stop();
        }
    }

}
