package org.df4j.core.node;

import org.df4j.core.node.AsyncAction;

/**
 * Actor is a reusable AsyncProc: after execution, it executes again as soon as new array of arguments is ready.
 */
public class Actor extends AsyncAction {

    @Override
    public void run() {
        try {
            blockStarted();
            runAction();
            if (!isStopped()) {
                purgeAll();
                start(); // restart execution
            }
        } catch (Throwable e) {
            result.completeExceptionally(e);
            stop();
        }
    }
}
