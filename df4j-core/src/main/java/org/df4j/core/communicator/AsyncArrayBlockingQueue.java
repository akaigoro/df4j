package org.df4j.core.communicator;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.port.InpChannel;
import org.df4j.core.port.OutFlow;
import org.df4j.protocol.Flow;
import org.df4j.protocol.ReverseFlow;
import org.reactivestreams.Subscriber;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 *  A {@link BlockingQueue} augmented with asynchronous interfaces to save and extract messages, and also interfaces to pass completion signal as required by  {@link Flow}.
 *
 * <p>
 *  Flow of messages:
 *  {@link ReverseFlow.Producer} =&gt; {@link AsyncArrayBlockingQueue}  =&gt; {@link Subscriber}
 *
 * @param <T> the type of the values passed through this token container
 */
public class AsyncArrayBlockingQueue<T> extends Actor implements
        /** asyncronous analogue of  {@link BlockingQueue#put(Object)} */
        ReverseFlow.Consumer<T>,
        /** asyncronous analogue of  {@link BlockingQueue#take()} */
        Flow.Publisher<T>
//        OutMessagePort<T>
{
    protected final InpChannel<T> inp;
    protected final OutFlow<T> out;
    private final Condition hasRoom;

    public AsyncArrayBlockingQueue(int capacity) {
        hasRoom = bblock.newCondition();
        inp = new InpChannel<>(this);
//        out = new OutFlow<>(transition, capacity, this);
        out = new OutFlow<>(this, capacity);
        start();
    }

    @Override
    public void subscribe(ReverseFlow.Producer<T> producer) {
        inp.subscribe(producer);
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        out.subscribe(s);
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
    public boolean offer(T token, long timeout, TimeUnit unit) throws InterruptedException {
        if (token == null) {
            throw new NullPointerException();
        }
        long millis = unit.toMillis(timeout);
        bblock.lock();
        try {
            for (;;) {
                if (completed) {
                    return false;
                }
                if (out.offer(token)) {
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
            bblock.unlock();
        }
    }

    public void put(T token) throws InterruptedException {
        if (token == null) {
            throw new NullPointerException();
        }
        bblock.lock();
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
            bblock.unlock();
        }
    }

    public boolean offer(T token) {
        return out.offer(token);
    }

    public T poll() throws InterruptedException {
        return out.poll();
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return out.poll(timeout, unit);
    }

    public T take() throws InterruptedException {
        return out.take();
    }

    @Override
    protected void runAction() throws Throwable {
        if (inp.isCompleted()) {
            out._onComplete(inp.getCompletionException());
        } else {
            T token = inp.remove();
            out.onNext(token);
        }
    }
}
