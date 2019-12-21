package org.df4j.core.port;

import org.df4j.core.actor.BasicBlock;
import org.df4j.protocol.ReverseFlow;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.df4j.protocol.Flow;

/**
 * A passive input paramerter,
 */
public class InpChannel<T> extends BasicBlock.Port implements ReverseFlow.Publisher<T> {
    protected volatile boolean completed;
    protected volatile Throwable completionException;
    protected volatile T value;
    protected Queue<ProducerSubscription> producers = new LinkedList<ProducerSubscription>();

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
    public void subscribe(ReverseFlow.Subscriber<T> producer) {
        ProducerSubscription subscription = new ProducerSubscription(producer);
        producer.onSubscribe(subscription);
    }

    public T remove() {
        plock.lock();
        try {
            T res;
            if (!isReady()) {
                throw new IllegalStateException();
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

    public T current() {
        plock.lock();
        try {
            return value;
        } finally {
            plock.unlock();
        }
    }


    class ProducerSubscription implements Flow.Subscription {
        protected ReverseFlow.Subscriber<T> producer;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        public ProducerSubscription(ReverseFlow.Subscriber<T> producer) {
            this.producer = producer;
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
