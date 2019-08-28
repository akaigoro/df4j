package org.df4j.core.actor;

import org.df4j.core.actor.Actor;

/**
 * does not restarts automatically.
 * To move to the next step,
 * method {@link #start()} must be called explicetly
 */
public abstract class LazyActor extends Actor {

    @Override
    public void run() {
        try {
            blockControl();
            runAction();
        } catch (Throwable e) {
            result.onError(e);
            stop();
        }
    }

}
