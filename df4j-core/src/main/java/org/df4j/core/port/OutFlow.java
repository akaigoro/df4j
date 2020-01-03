package org.df4j.core.port;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.BasicBlock;
import org.df4j.protocol.Flow;
import org.reactivestreams.*;

/**
 * A passive source of messages (like a server).
 * Unblocked initially.
 * It has room for single message.
 * Blocked when overflow.
 *
 * Because of complex logic, it is designaed as an Actor itself. However, it still controls firing of the parent actor.
 */
public class OutFlow<T> extends Actor implements Publisher<T>, OutMessagePort<T> {
    /** blocked when there is no more room for input messages */
    private BasicBlock.Port outerLock;
    private InpFlow<T> inp;
    private OutFlowSubscriptions subscriptions = new OutFlowSubscriptions();
    /** blocks when inp is full */

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public OutFlow(BasicBlock parent, int bufferCapacity) {
        super(parent.getDataflow());
        if (bufferCapacity <= 0) {
            throw new IllegalArgumentException();
        }
        inp = new InpFlowExt(bufferCapacity);
        parent.getDataflow().leave(); // keep reference to parent dataflow only for error propagation
        outerLock = new OuterLock(parent);
        setExecutor(parent.getExecutor());
        start();
    }

    public OutFlow(BasicBlock parent) {
        this(parent, 1);
    }

    private void debug(String s) {
 //       System.out.println(s);
    }

    public void subscribe(Subscriber subscriber) {
        subscriptions.subscribe(subscriber);
        if (inp.isCompleted()) {
            if (inp.isCompletedExceptionslly()) {
                subscriber.onError(inp.getCompletionException());
            } else {
                subscriber.onComplete();
            }
        }
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
    protected void runAction() {
        if (!inp.isCompleted()) {
            T token = inp.remove();
            OutFlowSubscriptions.OutFlowSubscription sub = subscriptions.remove();
            if (sub.remainedRequests <= 0) {
                throw new IllegalArgumentException();
            }
 //           debug(   " OutFlow: sub.remainedRequests = "+sub.remainedRequests+" sub.onNext: "+token);
            sub.onNext(token);
        } else{
            Throwable completionException = inp.getCompletionException();
            for (;;) {
                OutFlowSubscriptions.OutFlowSubscription sub = subscriptions.poll();
                if (sub == null) {
                    break;
                }
//                debug(" OutFlow: sub.onError "+completionException);
                sub.onError(completionException);
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
        public InpFlowExt(int buffCapacity) {
            super(OutFlow.this, buffCapacity);
        }

        @Override
        public synchronized void roomExhausted() {
            outerLock.block();
        }

        @Override
        public void roomAvailable() {
            outerLock.unblock();
        }
    }

    private class OutFlowSubscriptions extends InpFlood<OutFlowSubscriptions.OutFlowSubscription> {
        public OutFlowSubscriptions() {
            super(OutFlow.this);
        }

        public void subscribe(Subscriber subscriber) {
            OutFlowSubscription sub = new OutFlowSubscription(subscriber);
            subscriber.onSubscribe(sub);
        }

        @Override
        public void onNext(OutFlowSubscription sub) {
            plock.lock();
            try {
                if (sub.enqueued) {
                    return;
                }
                sub.enqueued = true;
                super.onNext(sub);
            } finally {
                plock.unlock();
            }
        }

        @Override
        public OutFlowSubscription remove() {
            plock.lock();
            try {
                OutFlowSubscription sub = super.remove();
                sub.enqueued = false;
                return sub;
            } finally {
                plock.unlock();
            }
        }

        @Override
        public boolean remove(OutFlowSubscription sub) {
            plock.lock();
            try {
                boolean res = super.remove(sub);
                sub.enqueued = false;
                return res;
            } finally {
                plock.unlock();
            }
        }

        private class OutFlowSubscription implements Flow.Subscription {
            // no own lock, use OutFlowSubscriptions.plock
            boolean enqueued = false;
            protected final Subscriber subscriber;
            private long remainedRequests = 0;
            private boolean cancelled = false;

            OutFlowSubscription(Subscriber subscriber) {
                this.subscriber = subscriber;
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }

            @Override
            public  void request(long n) {
                Throwable exception;
                plock.lock();
                getnext:
                try {
                    if (n <= 0) {
 //                       debug("  request: negative n");
                        exception = new IllegalArgumentException("request may not be negative");
                        break getnext;
                    }
                    if (cancelled) {
 //                       debug("   request: cancelled");
                        return;
                    }
  //                  debug("  request: remainedRequests = "+remainedRequests+" n = "+n);
                    remainedRequests += n;
                    subscriptions.onNext(this);
                    return;
                } finally {
                    plock.unlock();
                }
                subscriber.onError(exception);
            }

            @Override
            public void cancel() {
                plock.lock();
                try {
                    if (cancelled) {
                        return;
                    }
                    cancelled = true;
                    subscriptions.remove(this);
                } finally {
                    plock.unlock();
                }
            }

            public void onNext(T token) {
                plock.lock();
                try {
                    if (remainedRequests <= 0) {
                        throw new IllegalStateException();
                    }
                    remainedRequests--;
                    if (remainedRequests > 0) {
                        subscriptions.onNext(this);
                    }
                } finally {
                    plock.unlock();
                }
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
}
