package org.df4j.rxjava.port;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import org.df4j.core.actor.AsyncProc;
import org.df4j.core.port.OutFlow;
import org.reactivestreams.Subscriber;

public class OutFlowable extends Flowable {
    final AsyncProc parent;
    OutFlow out;

    public OutFlowable(AsyncProc parent) {
        this.parent = parent;
        out = new OutFlow(parent);
    }

    @Override
    protected void subscribeActual(@NonNull Subscriber s) {
        out.subscribe(s);
    }
}
