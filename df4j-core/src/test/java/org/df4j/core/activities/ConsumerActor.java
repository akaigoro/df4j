package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpChannel;

public class ConsumerActor extends Actor {
    final int delay;
    public InpChannel<Integer> inp = new InpChannel<>(this);

    public ConsumerActor(int delay, Dataflow parent) {
        super(parent);
        this.delay = delay;
    }

    public ConsumerActor(int delay) {
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
