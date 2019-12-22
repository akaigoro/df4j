package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.protocol.Pulse;

public class PulseListener extends BasicBlock.Port implements Pulse.Subscriber {

    public PulseListener(BasicBlock parent) {
        parent.super(true);
    }

    protected  void acquireFrom(Pulse.Publisher publisher) {
        plock.lock();
        try {
            publisher.subscribe(this);
        } finally {
            plock.unlock();
        }
    }

    @Override
    public  void awake() {
        plock.lock();
        try {
            unblock();
        } finally {
            plock.unlock();
        }
    }
}
