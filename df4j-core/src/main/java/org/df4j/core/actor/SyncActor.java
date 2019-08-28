package org.df4j.core.actor;

import org.df4j.core.actor.Actor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * runs synchronousely on the caller's thread
 */
public abstract class SyncActor extends Actor {
    AtomicInteger state = new AtomicInteger(0);

    @Override
    protected void fire() {
        if (state.getAndIncrement() > 0) {
            return;
        };
        for (;;) {
            try {
                run();
            } finally {
                if (state.decrementAndGet() == 0) {
                    return;
                }
            }
        }
    }
}
