package org.df4j.core.actors;

import org.df4j.core.actor.Actor;
import org.df4j.core.port.InpChannel;

public class Consumer extends Actor {
    final int delay;
    public InpChannel<Integer> inp = new InpChannel<>(this);

    public Consumer(int delay) {
        this.delay = delay;
    }

    @Override
    protected void runAction() throws Throwable {
        Thread.sleep(delay);
        if (inp.isCompleted()) {
            System.out.println(" completed.");
            stop();
        } else {
            Integer in = inp.remove();
            System.out.println(" got: "+in);
        }
    }
}
