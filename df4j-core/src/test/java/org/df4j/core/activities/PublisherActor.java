package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.OutFlow;

public class PublisherActor extends Actor {
    public OutFlow<Integer> out = new OutFlow<>(this);
    int cnt;
    final int delay;

    public PublisherActor(int cnt, int delay) {
        this.cnt = cnt;
        this.delay = delay;
    }

    public PublisherActor(Dataflow parent, int cnt, int delay) {
        super(parent);
        this.cnt = cnt;
        this.delay = delay;
    }

    @Override
    protected void runAction() throws Throwable {
        System.out.println("cnt: " + cnt);
        if (cnt == 0) {
            out.onComplete();
            stop();
        } else {
            out.onNext(cnt);
            cnt--;
            Thread.sleep(delay);
        }
    }
}
