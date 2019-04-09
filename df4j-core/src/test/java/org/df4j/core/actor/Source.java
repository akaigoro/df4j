package org.df4j.core.actor;

import org.df4j.core.asyncproc.AllOf;
import org.reactivestreams.Publisher;

public abstract class Source<T> extends Actor implements Publisher<T> {
    Logger log;

    public Source() {
    }

    public Source(Logger parent) {
        log = parent;
        parent.registerAsyncResult(asyncResult());
    }

    protected void println(String s) {
        if (log != null) {
            log.println(s);
        } else {
            System.out.println(s); // TODO remove
        }
    }

}
