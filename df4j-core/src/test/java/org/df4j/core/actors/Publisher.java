package org.df4j.core.actors;

import org.df4j.core.actor.Actor;
import org.df4j.core.port.OutMessage;

public class Publisher extends Actor {
    public OutMessage<Integer> out = new OutMessage<>(this);
    int cnt;
    final int delay;

    public Publisher(int cnt, int delay) {
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
