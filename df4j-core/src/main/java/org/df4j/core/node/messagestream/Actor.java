package org.df4j.core.node.messagestream;

import org.df4j.core.node.AsyncProcedure;

/**
 * Actor is a reusable AsyncProc: after execution, it executes again as soon as new array of arguments is ready.
 */
public class Actor extends AsyncProcedure<Void> {
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
