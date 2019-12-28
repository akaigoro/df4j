package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.port.OutChannel;
import org.df4j.protocol.ReverseFlow;

public class ProducerActor extends Actor {
    final int delay;
    int cnt;
    OutChannel<Integer> out;

    public ProducerActor(int cnt, ReverseFlow.Publisher<Integer> inp, int delay) {
        out = new OutChannel<>(this, inp);
        this.delay = delay;
        this.cnt = cnt;
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
