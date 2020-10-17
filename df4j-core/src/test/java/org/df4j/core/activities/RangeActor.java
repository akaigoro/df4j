package org.df4j.core.activities;


import org.df4j.core.dataflow.Actor;
import org.df4j.core.port.OutFlow;

public class RangeActor extends Actor {

    int to;
    int cnt;
    OutFlow<Integer> out = new OutFlow<Integer>(this);

    public RangeActor(int from, int to) {
        this.cnt = from;
        this.to = to;
        start();
    }

    @Override
    protected void runAction() throws Throwable {
        if (isCompleted()) {
            Throwable completionException1 = getCompletionException();
            if (completionException1 == null) {
                out.onComplete();
            } else {
                out.onError(completionException1);
            }
            return;
        }
        if (cnt == to) {
            complete();
            out.onComplete();
            return;
        }
        out.onNext(cnt);
        cnt++;
    }

}
