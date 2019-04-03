package org.df4j.core.reactivestream;

import org.df4j.core.actor.Actor;
import org.df4j.core.asynchproc.AllOf;
import org.reactivestreams.Publisher;

public abstract class Source<T> extends Actor implements Publisher<T> {

    public Source() {
    }

    public Source(AllOf parent) {
        parent.registerAsyncResult(asyncResult());
    }

}
