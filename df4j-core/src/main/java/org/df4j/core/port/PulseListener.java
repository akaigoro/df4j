package org.df4j.core.port;

import org.df4j.core.actor.BasicBlock;
import org.df4j.protocol.SignalStream;

public class PulseListener extends BasicBlock.Port implements SignalStream.Subscriber {
    protected SignalStream.Publisher publisher;

    public PulseListener(BasicBlock parent, SignalStream.Publisher publisher) {
        parent.super(true);
        this.publisher = publisher;
    }

    public PulseListener(BasicBlock parent) {
        parent.super(false);
    }

    protected  void acquireFrom(SignalStream.Publisher publisher) {
        plock.lock();
        try {
            publisher.subscribe(this);
        } finally {
            plock.unlock();
        }
    }

    protected  void acquire() {
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
