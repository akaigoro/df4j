package org.df4j.examples.reactive;

import org.df4j.core.Actor;
import org.df4j.core.StreamPort;

import java.util.concurrent.atomic.AtomicLong;

/**
 * an actor with input and output transition which implement Subscriber and Publisher
 */
public abstract class Reactor extends Actor {

    protected class StreamSubscriber<T> extends AbstractSubscriber<T> implements Flow.Subscriber<T> {
        protected final StreamInput<T> input = new StreamInput<>();

        public StreamSubscriber() {
            super(10);
        }

        public StreamSubscriber(long bufferSize) {
            super(bufferSize);
        }

        public T get() {
            return input.get();
        }

        @Override
        protected void act(T item) {
            input.post(item);
        }

        @Override
        public void onComplete() {
            input.close();
        }
    }

    /**
     * serves maximum one subscriber
     *
     * @param <T>
     */
    class SingleStreamPublisher<T> extends Pin implements Flow.Publisher<T>, Flow.Subscription, StreamPort<T> {
        protected Flow.Subscriber<? super T> subscriber = null;
        protected boolean completed;
        /**
         * max number of messages this actor can send while single activation
         */
        protected final long reserv;

        /**
         * remained number of messages this actor can send during current activation
         * Actor is not activated if allowed < reserv
         */
        protected AtomicLong allowed = new AtomicLong(0);

        public SingleStreamPublisher(long reserv) {
            this.reserv = reserv;
        }

        public SingleStreamPublisher() {
            this(1);
        }

        /**
         * does nothing: counter decreases when a message is posted
         */
        @Override
        protected void _purge() {
        }

        @Override
        public void subscribe(Flow.Subscriber<? super T> subscriber2) {
            if (subscriber != null)
                subscriber2.onError(new IllegalStateException()); // only one allowed
            else {
                subscriber = subscriber2;
                subscriber.onSubscribe(this);
            }
        }

        public void request(long n) {
            long prev, next;
            do {
                prev = allowed.get();
                next = prev + n;
            } while (!allowed.compareAndSet(prev, next));
            boolean doFire = false;
            if (prev < reserv && next >= reserv) {
                doFire = _turnOn();
            }
            if (doFire) {
                transition.fire();
            }
        }

        @Override
        public void post(T message) {
            subscriber.onNext(message);
            long prev, next;
            do {
                prev = allowed.get();
                next = prev - 1;
            } while (!allowed.compareAndSet(prev, next));
            if (prev >= reserv && next < reserv) {
                _turnOff();
            }
        }

        @Override
        public void close() {

        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void cancel() {
            completed = true;
        }
    }
}
