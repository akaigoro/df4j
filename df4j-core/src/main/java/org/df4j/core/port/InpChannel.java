package org.df4j.core.port;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.actor.AsyncProc;
import org.df4j.core.util.linked.LinkImpl;
import org.df4j.core.util.linked.LinkedQueue;
import org.df4j.protocol.ReverseFlow;
import org.reactivestreams.Publisher;

import java.util.ArrayDeque;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * A passive input parameter.
 * Has room for single value.
 */
public class InpChannel<T> extends CompletablePort implements ReverseFlow.Consumer<T>, InpMessagePort<T> {
    protected int capacity;
    private LinkedQueue<ProducerSubscription> activeSubscriptions = new LinkedQueue<>();
    private LinkedQueue<ProducerSubscription> passiveSubscriptions = new LinkedQueue<>();
    protected ArrayDeque<T> tokens;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     * @param capacity max buffer capacity
     */
    public InpChannel(AsyncProc parent, int capacity) {
        super(parent);
        this.capacity = capacity;
        tokens = new ArrayDeque<>(capacity);
    }

    public InpChannel(AsyncProc parent) {
        this(parent, 8);
    }

    private boolean _tokensFull() {
        return size() == capacity;
    }

    public boolean isCompleted() {
        return completed && tokens.size() == 0;
    }

    public Throwable getCompletionException() {
        if (isCompleted()) {
            return completionException;
        } else {
            return null;
        }
    }

    @Override
    public void feedFrom(ReverseFlow.Producer<T> producer) {
        new ProducerSubscription(producer);
    }

    public void feedFrom(Publisher<T> publisher) {
        new ProducerSubscription(publisher);
    }

    public synchronized boolean offer(T token) {
        if (completed) {
            return false;
        }
        if (_tokensFull()) {
            return false;
        }
        tokens.add(token);
        unblock();
        return true;
    }

    /**
     * @return the value received from a subscriber, or null if no value was received yet or that value has been removed.
     */
    public synchronized T current() {
        return tokens.peek();
    }

    @Override
    public synchronized void block() {
        if (completed) {
            return;
        }
        super.block();
    }

    /**
     * removes and returns incoming value if it is present
     * @return the value received from a subscriber, or null if no value has been received yet or that value has been removed.
     */
    public synchronized T poll() {
        T res;
        if (tokens.isEmpty()) {
            return null;
        }
        res = tokens.poll();
        notifyAll();

        ProducerSubscription client = activeSubscriptions.peek();
        if (client == null) {
            if (tokens.isEmpty() && !completed) {
                block();
            }
            return res;
        }
        ReverseFlow.Producer<T> subscriber = client.producer;
        if (!subscriber.isCompleted()) {
            T token = subscriber.remove();
            if (token == null) {
                subscriber.onError(new IllegalArgumentException());
            }
            tokens.add(token);
            client.remainedRequests--;
            if (client.remainedRequests == 0) {
                activeSubscriptions.remove(client);
                passiveSubscriptions.add(client);
            }
        } else {
            completed = true;
            completionException = subscriber.getCompletionException();
        }
        unblock();
        return res;
    }

    public void add(T token) {
        if (token == null) {
            throw new IllegalArgumentException();
        }
        if (!offer(token)) {
            throw new IllegalStateException();
        }
    }

    public synchronized void put(T token) throws InterruptedException {
        if (token == null) {
            throw new NullPointerException();
        }
        for (;;) {
            if (completed) {
                throw new IllegalStateException();
            }
            if (offer(token)) {
                return;
            }
            wait();
        }
    }

    /**
     * @return the value received from a subscriber
     * @throws IllegalStateException if no value has been received yet or that value has been removed.
     */
    public synchronized T remove() throws CompletionException {
        if (tokens.isEmpty()) {
            if (completed) {
                throw new CompletionException("Port already completed", completionException);
            } else {
                throw new IllegalStateException();
            }
        }
        return poll();
    }

    public int size() {
        return tokens.size();
    }

    protected class ProducerSubscription extends LinkImpl implements ReverseFlow.ReverseFlowSubscription<T> {
        protected final ReverseFlow.Producer<T> producer;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        ProducerSubscription(ReverseFlow.Producer<T> producer) {
            this.producer = producer;
            synchronized(this) {
                passiveSubscriptions.add(this);
            }
            producer.onSubscribe(this);
        }

        ProducerSubscription(Publisher<T> publisher) {
            PortAdapter<T> adapter = new PortAdapter<T>(getParentActor().getActorGroup());
            publisher.subscribe(adapter.inp);
            this.producer = adapter.out;
            synchronized(this) {
                passiveSubscriptions.add(this);
            }
            adapter.out.onSubscribe(this);
        }

        /**
         *
         * @param n the increment of demand
         */
        @Override
        public synchronized void request(long n) {
            if (n <= 0) {
                producer.onError(new IllegalArgumentException());
                return;
            }
            if (cancelled) {
                return;
            }
            if (completed) {
                return;
            }
            boolean wasActive = remainedRequests > 0;
            remainedRequests += n;
            if (wasActive) {
                return; //  is active already
            }
            if (transfer()) return;
            if (remainedRequests > 0) {
                passiveSubscriptions.remove(this);
                activeSubscriptions.add(this);
            }
            unblock();
        }

        private boolean transfer() {
            while (!_tokensFull() && remainedRequests > 0) {
                if (producer.isCompleted()) {
                    _onComplete(producer.getCompletionException());
                } else {
                    T token = producer.remove();
                    if (token == null) {
                        // wrong subscriber
                        producer.onError(new IllegalArgumentException());
                        _cancel();
                        return true;
                    }
                    tokens.add(token);
                    remainedRequests--;
                }
            }
            return false;
        }

        @Override
        public boolean offer(T token) {
            return InpChannel.this.offer(token);
        }

        private synchronized void _onComplete(Throwable throwable) {
            if (cancelled) {
                return;
            }
            cancelled = true;
            InpChannel.this._onComplete(throwable);
        }

        @Override
        public void onComplete() {
            _onComplete(null);
        }

        @Override
        public void onError(Throwable throwable) {
            _onComplete(throwable);
        }

        @Override
        public synchronized void cancel() {
            _cancel();
        }

        private boolean _cancel() {
            if (cancelled) {
                return true;
            }
            cancelled = true;
            if (completed) {
                return true;
            }
            if (remainedRequests > 0) {
                activeSubscriptions.remove(this);
            } else {
                passiveSubscriptions.remove(this);
            }
            return false;
        }
    }

    static class PortAdapter<T> extends Actor {
        InpFlow<T> inp = new InpFlow<>(this);
        OutChannel<T> out = new OutChannel<>(this);

        public PortAdapter(ActorGroup parent) {
            super(parent);
        }

        @Override
        protected void fire() {
            run();
        }

        @Override
        protected void runAction() throws Throwable {
            if (inp.isCompleted()) {
                if (inp.isCompletedExceptionally()) {
                    out.onError(inp.getCompletionException());
                } else {
                    out.cancel();
                }
                complete();
            } else {
                out.onNext(inp.remove());
            }
        }
    }
}
