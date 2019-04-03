package org.df4j.core.actor.ext;

import org.df4j.core.actor.Actor;

public abstract class LazyActor extends Actor {

    @Override
    public void run() {
        try {
            blockStarted();
            runAction();
        } catch (Throwable e) {
            result.completeExceptionally(e);
            stop();
        }
    }

}
