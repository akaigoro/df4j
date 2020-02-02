package org.df4j.core.communicator;

import org.df4j.core.base.OutFlowBase;
import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.BasicBlock;
import org.df4j.core.port.InpChannel;
import org.df4j.core.port.OutFlow;
import org.df4j.core.port.OutMessagePort;
import org.df4j.protocol.Flow;
import org.df4j.protocol.ReverseFlow;
import org.reactivestreams.Subscriber;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  A {@link BlockingQueue} augmented with asynchronous interfaces to save and extract messages, and also interfaces to pass completion signal as required by  {@link Flow}.
 *
 * <p>
 *  Flow of messages:
 *  {@link ReverseFlow.Producer} =&gt; {@link AsyncArrayBlockingQueue}  =&gt; {@link Subscriber}
 *
 * @param <T> the type of the values passed through this token container
 */
public class AsyncArrayBlockingQueue<T> extends OutFlowBase<T> implements BlockingQueue<T>,
        /** asyncronous analogue of  {@link BlockingQueue#put(Object)} */
        ReverseFlow.Consumer<T>,
        /** asyncronous analogue of  {@link BlockingQueue#take()} */
        Flow.Publisher<T>, OutMessagePort<T> {
    protected final Actor actor;
    public InpChannel<T> inp;
    protected final OutFlow<T> out;
    private final Condition hasRoom;

    public AsyncArrayBlockingQueue(int capacity) {
        super(new ReentrantLock(), capacity);
        actor = new MyActor();
        actor.start();
        inp = new InpChannel<>(actor);
        out = new OutFlow<>(actor, capacity);
        hasRoom = qlock.newCondition();
    }

    @Override
    public void offer(ReverseFlow.Producer<T> producer) {
        inp.offer(producer);
    }

    /**
     *  If there are subscribers waiting for tokens,
     *  then the first subscriber is removed from the subscribers queue and is fed with the token,
     *  otherwise, the token is inserted into this queue, waiting up to the
     *  specified wait time if necessary for space to become available.
     *
     * @param token the element to add
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return {@code true} if successful, or {@code false} if
     *         the specified waiting time elapses before space is available
     * @throws InterruptedException if interrupted while waiting
     */
    @Override

    public boolean offer(T token, long timeout, TimeUnit unit) throws InterruptedException {
        long millis = unit.toMillis(timeout);
        FlowSubscriptionImpl sub;
        qlock.lock();
        try {
            for (;;) {
                if (completed) {
                    return false;
                }
                if (offer(token)) {
                    return true;
                }
                if (millis <= 0) {
                    return false;
                }
                long targetTime = System.currentTimeMillis() + millis;
                hasRoom.await(millis, TimeUnit.MILLISECONDS);
                millis = targetTime - System.currentTimeMillis();
            }
        } finally {
            qlock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        qlock.lock();
        try {
            return capacity - tokens.size();
        } finally {
            qlock.unlock();
        }
    }

    @Override
    public void put(T token) throws InterruptedException {
        qlock.lock();
        try {
            for (;;) {
                if (completed) {
                    throw new IllegalStateException();
                }
                if (offer(token, 1, TimeUnit.DAYS)) {
                    return;
                }
            }
        } finally {
            qlock.unlock();
        }
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void hasRoomEvent() {
        out.unblock();
        hasRoom.signalAll();
    }

    @Override
    protected void noRoomEvent() {
        out.block();
    }

    @Override
    public void onNext(T message) {
        if (!offer(message)) {
            throw new IllegalStateException("buffer overflow");
        }
    }

    private class MyActor extends Actor {

        @Override
        protected void fire() {
            run();
        }

        @Override
        protected void runAction() throws Throwable {
            inp.extractTo(AsyncArrayBlockingQueue.this);
        }
    }

    class MyOutFlow<T> extends BasicBlock.Port {

        public MyOutFlow(BasicBlock parent) {
            parent.super(true);
        }
    }

}
