package org.df4j.core.activities;

import lombok.Getter;
import org.df4j.core.actor.Actor;
import org.df4j.core.port.OutFlow;

public class RangeActor extends Actor {
    int to;
    int cnt;
    @Getter
    private OutFlow<Integer> out = new OutFlow<>(this);

    public RangeActor(int from, int to) {
        this.cnt = from;
        this.to = to;
        start();
    }

    @Override
    protected void whenComplete(Throwable e) {
        if (e == null) {
            out.onComplete();
        } else {
            out.onError(e);
        }
    }

    @Override
    protected void runAction() {
        if (isCompleted()) {
            complete(getCompletionException());
            return;
        }
        if (cnt == to) {
            complete();
            return;
        }
        out.onNext(cnt);
        cnt++;
    }

}
