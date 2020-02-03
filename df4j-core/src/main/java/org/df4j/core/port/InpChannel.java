package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.protocol.FlowSubscription;
import org.df4j.protocol.ReverseFlow;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A passive input parameter.
 * Has room for single value.
 */
public class InpChannel<T> extends BasicBlock.Port implements ReverseFlow.Consumer<T>, InpMessagePort<T> {
    protected volatile boolean completed;
    protected volatile Throwable completionException;
    protected volatile T value;
    protected Queue<ProducerSubscription> producers = new LinkedList<ProducerSubscription>();

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public InpChannel(BasicBlock parent) {
        parent.super(false);
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
    public void suck(ReverseFlow.Producer<T> producer) {
        ProducerSubscription subscription = new ProducerSubscription(producer);
        producer.onSubscribe(subscription);
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
            T value = this.value;
            this.value = null;
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
            ProducerSubscription client = producers.poll();
            if (client == null) {
                value = null;
                block();
            } else {
                client.remove();
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

    class ProducerSubscription implements FlowSubscription {
        protected ReverseFlow.Producer<T> producer;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        public ProducerSubscription(ReverseFlow.Producer<T> producer) {
            this.producer = producer;
        }

        @Override
        public boolean isCancelled() {
            plock.lock();
            try {
                return cancelled;
            } finally {
                plock.unlock();
            }
        }

        /**
         * @param n number of messages the producer is able to dekiver now
         */
        @Override
        public void request(long n) {
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            plock.lock();
            try {
                if (cancelled) {
                    return;
                }
                if (completed) {
                    producer.cancel();
                    return;
                }
                if (remainedRequests > 0) {
                    remainedRequests += n;
                    return;
                }
                remainedRequests = n;
                if (value != null) {
                    producers.add(this);
                    return;
                }
                remove();
            } finally {
                plock.unlock();
            }
        }

        private void remove() {
            value = producer.remove();
            if (value != null) {
                remainedRequests--;
                if (remainedRequests > 0) {
                    producers.add(this);
                }
            } else {
                completed = producer.isCompleted();
                completionException = producer.getCompletionException();
            }
            unblock();
        }

        @Override
        public void cancel() {
            plock.lock();
            try {
                producers.remove(this);
                cancelled = true;
            } finally {
                plock.unlock();
            }
        }
    }

}
