package org.df4j.core.reactivestream;

import org.df4j.core.tasknode.messagescalar.AllOf;
import org.df4j.core.tasknode.messagestream.Actor;
import org.reactivestreams.Publisher;

abstract class Source<T> extends Actor implements Publisher<T> {

    public Source() {
    }

    public Source(AllOf parent) {
        parent.registerAsyncResult(asyncResult());
    }

}
