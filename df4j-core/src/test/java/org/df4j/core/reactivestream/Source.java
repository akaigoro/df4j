package org.df4j.core.reactivestream;

import org.df4j.core.scalar.AllOf;
import org.df4j.core.stream.Actor;
import org.reactivestreams.Publisher;

public abstract class Source<T> extends Actor implements Publisher<T> {

    public Source() {
    }

    public Source(AllOf parent) {
        parent.registerAsyncResult(asyncResult());
    }

}
