package org.df4j.core.actor;

import java.util.concurrent.Flow;

import static org.df4j.core.util.Utils.sneakyThrow;

public abstract class Source<T> extends Actor implements Flow.Publisher<T> {
    Logger log;

    public Source() {
    }

    public Source(Logger parent) {
        log = parent;
    }

    protected void println(String s) {
        if (log != null) {
            log.println(s);
        } else {
            System.out.println(s); // TODO remove
        }
    }

    @Override
    public synchronized void stopExceptionally(Throwable t) {
        super.stopExceptionally(t);
        sneakyThrow(t);
    }
}
