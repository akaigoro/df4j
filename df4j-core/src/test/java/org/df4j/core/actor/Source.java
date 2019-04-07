package org.df4j.core.actor;

import org.df4j.core.asyncproc.AllOf;
import org.reactivestreams.Publisher;

public abstract class Source<T> extends Actor implements Publisher<T> {

    public Source() {
    }

    public Source(AllOf parent) {
        parent.registerAsyncResult(asyncResult());
    }

}
