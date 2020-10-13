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
        if (checkComplete()) return;
        out.onNext(cnt);
        cnt++;
    }

    private boolean checkComplete() {
        if (isCompleted()) {
            Throwable completionException = getCompletionException();
            if (completionException == null) {
                out.onComplete();
            } else {
                out.onError(completionException);
            }
            return true;
        }
        if (cnt == to) {
            complete();
            out.onComplete();
            return true;
        }
        return false;
    }
}
