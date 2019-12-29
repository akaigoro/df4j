package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.OutChannel;

public class ProducerActor extends Actor {
    final int delay;
    int cnt;
    public OutChannel<Integer> out;

    public ProducerActor(Dataflow parent, int cnt, int delay) {
        super(parent);
        out = new OutChannel<>(this);
        this.cnt = cnt;
        this.delay = delay;
    }

    public ProducerActor(int cnt, int delay) {
        out = new OutChannel<>(this);
        this.cnt = cnt;
        this.delay = delay;
    }

    @Override
    protected void runAction() throws Throwable {
        System.out.println("cnt: "+cnt);
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
