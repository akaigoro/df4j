package org.df4j.core.port;

import org.df4j.core.base.OutFlowBase;
import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.util.linked.Link;
import org.df4j.core.util.linked.LinkImpl;
import org.df4j.core.util.linked.LinkedQueue;
import org.df4j.protocol.FlowSubscription;
import org.df4j.protocol.ReverseFlow;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A passive input parameter.
 * Has room for single value.
 */
public class InpChannel<T> extends AsyncProc.Port implements ReverseFlow.Consumer<T>, InpMessagePort<T> {
    private LinkedQueue<ProducerSubscription> activeProducers = new LinkedQueue<>();
    private LinkedQueue<ProducerSubscription> passiveProducers = new LinkedQueue<>();
    protected volatile boolean completed;
    protected volatile Throwable completionException;
    protected volatile T value;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public InpChannel(AsyncProc parent) {
        parent.super(false);
    }

    public InpChannel(AsyncProc parent, int capacity) {
        this(parent);
    }

    public boolean isCompleted() {
        plock.lock();
        try {
            return completed;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public Throwable getCompletionException() {
        plock.lock();
        try {
            if (isCompleted()) {
                return completionException;
            } else {
                return null;
            }
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void subscribe(ReverseFlow.Producer<T> producer) {
        ProducerSubscription subscription = new ProducerSubscription(producer);
        passiveProducers.add(subscription);
        producer.onSubscribe(subscription);
    }


    public void _onComplete(Throwable cause) {
        plock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            completionException = cause;
            unblock();
        } finally {
            plock.unlock();
        }
    }

    public void onComplete() {
        _onComplete(null);
    }

    public void onError(Throwable cause) {
        _onComplete(cause);
    }

    /**
     * @return the value received from a subscriber, or null if no value was received yet or that value has been removed.
     */
    public T current() {
        plock.lock();
        try {
            return value;
        } finally {
            plock.unlock();
        }
    }

    /**
     * @return the value received from a subscriber, or null if no value was received yet or that value has been removed.
     */
    public T remove() {
        plock.lock();
        try {
            if (!ready) {
                throw new IllegalStateException();
            }
            ready = false;
            T value = this.value;
            this.value = null;
            _block();
            return value;
        } finally {
            plock.unlock();
        }
    }

    /**
     * removes and returns incoming value if it is present
     * @return the value received from a subscriber, or null if no value has been received yet or that value has been removed.
     */
    public T poll() {
        plock.lock();
        try {
            T res;
            if (!ready) {
                return null;
            }
            res = value;
            ProducerSubscription client = activeProducers.poll();
            if (client == null) {
                value = null;
                block();
            } else {
                if (!client.subscriber.isCompleted()) {
                    value = client.subscriber.remove();
                    client.remainedRequests--;
                    if (client.remainedRequests > 0) {
                        activeProducers.add(client);
                    } else {
                        passiveProducers.add(client);
                    }
                } else {
                    completed = true;
                    completionException = client.subscriber.getCompletionException();
                }
                unblock();
            }
            return res;
        } finally {
            plock.unlock();
        }
    }

    /**
     *
     * @return the value received from a subscriber
     * @throws IllegalStateException if no value has been received yet or that value has been removed.
     */
    public T removeAndRequest() {
        plock.lock();
        try {
            if (!isReady()) {
                throw new IllegalStateException();
            }
            return poll();
        } finally {
            plock.unlock();
        }
    }

    public void extractTo(OutMessagePort<T> out) {
        if (!isCompleted()) {
            out.onNext(removeAndRequest());
        } else if (completionException == null) {
            out.onComplete();
        } else {
            out.onError(completionException);
        }
    }

    protected class ProducerSubscription extends LinkImpl<ProducerSubscription> implements ReverseFlow.ReverseFlowSubscription {
        private final Lock slock = new ReentrantLock();
        protected final ReverseFlow.Producer<T> subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        ProducerSubscription(ReverseFlow.Producer subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public ProducerSubscription getItem() {
            return this;
        }

        @Override
        public boolean isCancelled() {
            slock.lock();
            try {
                return cancelled;
            } finally {
                slock.unlock();
            }
        }

        /**
         *
         * @param n the increment of demand
         */
        @Override
        public void request(long n) {
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                if (completed) {
                    return;
                }
                remainedRequests += n;
                if (remainedRequests > n) {
                    return;
                }
                plock.lock();
                try {
                    passiveProducers.remove(this);
                    activeProducers.add(this);
                } finally {
                    plock.unlock();
                }
            } finally {
                slock.unlock();
            }
            unblock();
        }

        public Link<ProducerSubscription> getNext() {
            return super.getNext();
        }

        @Override
        public void onError(Throwable throwable) {
            _onComplete(throwable);
        }

        private void _onComplete(Throwable throwable) {
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                InpChannel.this._onComplete(throwable);
            } finally {
                slock.unlock();
            }
        }

        @Override
        public void onComplete() {
            _onComplete(null);
        }

        @Override
        public void cancel() {
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                plock.lock();
                try {
                    if (completed) {
                        return;
                    }
                    if (remainedRequests > 0) {
                        activeProducers.remove(this);
                    } else {
                        passiveProducers.remove(this);
                    }
                } finally {
                    plock.unlock();
                }
            } finally {
                slock.unlock();
            }
        }
    }
}
