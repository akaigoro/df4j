package org.df4j.core.port;

import org.df4j.core.actor.BasicBlock;
import org.df4j.core.protocol.SignalStream;

public class PulseListener extends BasicBlock.Port implements SignalStream.Subscriber {
    protected SignalStream.Publisher publisher;

    public PulseListener(BasicBlock parent, SignalStream.Publisher publisher) {
        parent.super(true);
        this.publisher = publisher;
    }

    public PulseListener(BasicBlock parent) {
        parent.super(false);
    }

    protected synchronized void acquireFrom(SignalStream.Publisher publisher) {
        publisher.subscribe(this);
    }

    protected synchronized void acquire() {
        publisher.subscribe(this);
    }

    @Override
    public synchronized void awake() {
        unblock();
    }
}
