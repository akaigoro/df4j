package org.df4j.core.port;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.BasicBlock;
import org.df4j.core.util.Utils;
import org.df4j.protocol.Flow;
import org.df4j.protocol.FlowSubscription;
import org.reactivestreams.Subscriber;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A passive source of messages (like a server).
 * Unblocked initially.
 * It has room for single message.
 * Blocked when overflow.
 *
 * Because of complex logic, it is designaed as an Actor itself. However, it still controls firing of the parent actor.
 */
public class OutFlow<T> extends Actor implements Flow.Publisher<T> {
    private BasicBlock.Port outerLock;
    private InpFlow<T> inp = new InpFlowExt();
    private InpFlood<OutFlowSubscription> subscriptions = new InpFlood(this);
    /** blocks when inp is full */

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public OutFlow(BasicBlock parent) {
        super(parent.getDataflow());
        parent.getDataflow().leave(); // keep reference to parent dataflow only for error propagation
        outerLock = new OuterLock(parent);
        setExecutor(parent.getExecutor());
        start();
    }

    public void subscribe(Flow.Subscriber subscriber) {
        OutFlowSubscription sub = new OutFlowSubscription(subscriber);
        subscriptions.onNext(sub);
        subscriber.onSubscribe(sub);
    }

    public void onNext(T t) {
        inp.onNext(t);
    }

    public void onComplete() {
        inp.onComplete();
    }

    public void onError(Throwable t) {
        inp.onError(t);
    }

    @Override
    protected void runAction() throws Throwable {
        if (!inp.isCompleted()) {
            T token = inp.remove();
            OutFlowSubscription sub = subscriptions.current();
            if (sub.remainedRequests == 1) {
                sub.remainedRequests = 0;
                subscriptions.remove();
            } else {
                sub.remainedRequests--;
            }
            sub.onNext(token);
        } else{
            for (;;) {
                OutFlow.OutFlowSubscription sub = subscriptions.poll();
                if (sub == null) {
                    break;
                }
                sub.onError(inp.getCompletionException());
            }
            stop();
        }
    }

    private static class OuterLock extends Port {
        public OuterLock(BasicBlock parent) {
            parent.super(true);
        }

        @Override
        public synchronized void block() {
            super.block();
        }
    }

    private class InpFlowExt extends InpFlow<T> {
        public InpFlowExt() {
            super(OutFlow.this);
        }

        @Override
        public synchronized void block() {
            super.block();
            outerLock.unblock();
        }

        @Override
        public void unblock() {
            super.unblock();
            outerLock.block();
        }
    }

    private class OutFlowSubscription implements FlowSubscription {
        private final Lock slock = new ReentrantLock();
        protected final Flow.Subscriber subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        OutFlowSubscription(Flow.Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public  void request(long n) {
            Throwable exception;
            slock.lock();
            getnext:
            try {
                if (n <= 0) {
                    exception = new IllegalArgumentException("request may not be negative");
                    break getnext;
                }
                if (cancelled) {
                    return;
                }
                if (remainedRequests > 0) { //we are already waiting in the queue
                    remainedRequests += n;
                    return;
                }
                if (inp.isReady()) {
                    subscriptions.onNext(this);
                    return;
                }
                if (inp.isCompleted()) {
                    exception = inp.getCompletionException();
                } else {
                    return;
                }
            } finally {
                slock.unlock();
            }
            if (exception == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(exception);
            }

        }

        @Override
        public void cancel() {
            slock.lock();
            getnext:
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                subscriptions.remove(this);
            } finally {
                slock.unlock();
            }

        }

        public void onNext(T token) {
            subscriber.onNext(token);
        }

        public void onError(Throwable completionException) {
            if (completionException == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(completionException);
            }
        }
    }
}
