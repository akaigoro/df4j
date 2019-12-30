package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.protocol.Flow;
import org.df4j.protocol.SignalFlow;

import java.util.TimerTask;

/**
 * asynchronous receiver of permit flow from a {@link SignalFlow.Publisher}, e.g. {@link org.df4j.core.communicator.AsyncSemaphore}.
 */
public class InpSignal extends BasicBlock.Port implements SignalFlow.Subscriber {
    private Flow.Subscription subscription;

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public InpSignal(BasicBlock parent) {
        parent.super(false);
    }

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     * @param publisher a  {@link SignalFlow.Publisher} to subscribe
     */
    public InpSignal(BasicBlock parent, SignalFlow.Publisher publisher) {
        parent.super(true);
        publisher.subscribe(this);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        plock.lock();
        try {
            this.subscription = subscription;
            subscription.request(1);
        } finally {
            plock.unlock();
        }
    }

    @Override
    public  void awake() {
        Dataflow dataflow = super.getDataflow();
        if (dataflow.isCompleted()) {
            return;
        }
        plock.lock();
        try {
            unblock();
            subscription.request(1);
        } finally {
            plock.unlock();
        }
    }

    /**
     * awakes this port after delay
     * @param delay time delay in milliseconds
     */
    public void awake(long delay) {
        Dataflow dataflow = super.getDataflow();
        if (dataflow.isCompleted()) {
            return;
        }
        TimerTask task = new TimerTask(){
            @Override
            public void run() {
                awake();
            }
        };
        dataflow.getTimer().schedule(task, delay);
    }
}
