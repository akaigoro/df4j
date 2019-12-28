package org.df4j.core.portadapter;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;

public class ReactiveProcessor extends Actor {
    InpReact<Integer> inp = new InpReact<>(this);
    public OutReact<Integer> out = new OutReact<>(this);
    final int delay;

    public ReactiveProcessor(Dataflow parent, int delay) {
        super(parent);
        this.delay = delay;
    }

    @Override
    protected void runAction() throws Throwable {
        if (!inp.isCompleted()) {
            out.onNext(inp.remove());
        } else {
            if (!inp.isCompletedExceptionslly()) {
                out.onComplete();
            } else {
                out.onError(inp.getCompletionException());
            }
            stop();
        }
    }
}
