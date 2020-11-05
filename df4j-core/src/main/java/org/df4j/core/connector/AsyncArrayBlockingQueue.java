package org.df4j.core.connector;

import org.df4j.core.actor.Actor;
import org.df4j.core.port.InpMultiFlow;
import org.df4j.core.port.OutFlow;
import org.df4j.protocol.Flow;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *  Partially implemented {@link BlockingQueue} augmented with asynchronous interfaces to save and extract messages,
 *  and also interfaces to pass completion signal as required by {@link Flow}.
 *
 * <p>
 *  Flow of messages:
 *  {@link Flow.Publisher} =&gt; {@link AsyncArrayBlockingQueue}  =&gt; {@link Subscriber}
 *
 * @param <T> the type of the values passed through this token container
 */
public class AsyncArrayBlockingQueue<T> extends Actor implements
        /** asyncronous analogue of  {@link BlockingQueue#take()} */
        Flow.Source<T>, Flow.Publisher<T>
{
    protected final InpMultiFlow<T> inp;
    protected final OutFlow<T> out;

    public AsyncArrayBlockingQueue(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException();
        }
        inp = new InpMultiFlow<>(this, capacity);
        out = new OutFlow<>(this);
        start();
    }

    public AsyncArrayBlockingQueue() {
        this(32);
    }

    public void subscribeTo(Publisher<T> publisher) {
        inp.subscribeTo(publisher);
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        if (isCompleted()) {
            Throwable completionException = getCompletionException();
            if (completionException == null) {
                s.onComplete();
            } else {
                s.onError(completionException);
            }
        } else {
            out.subscribe(s);
        }
    }

    /**
     * Inserts next token
     * @param token token to insert
     * @return false when input buffer is full
     */
    public boolean offer(T token) {
        return inp.offer(token);
    }

    /**
     * Inserts next token
     * @param token token to insert
     * @throws IllegalStateException when input buffer is full
     */
    public void add(T token) {
        inp.add(token);
    }

    /**
     * Inserts next token
     * blocks when input buffer is full
     * @param token token to insert
     * @throws InterruptedException
     *    when inerrupted
     */
    public void put(T token) throws InterruptedException {
        inp.put(token);
    }

    public int size() {
        return inp.size();
    }

    /**
     * extracts next token
     * @return  token
     * @throws IllegalStateException when the buffer is empty
     */
    public synchronized T remove() {
        T res = inp.remove();
        if (inp.isCompleted()) {
            complete(inp.getCompletionException());
        }
        return res;
    }

    /**
     * extracts next token
     * never blocks
     * @return  token
     *       or null when the buffer is empty
     */
    public T poll() {
        return inp.poll();
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        long targetTime=System.nanoTime()+unit.toNanos(timeout);
        synchronized (inp) {
            for (;;) {
                T res = inp.poll();
                if (res != null) {
                    return res;
                }
                long timeoutNanos=targetTime-System.nanoTime();
                if (timeoutNanos <= 0) {
                    throw new TimeoutException();
                }
                inp.wait(timeoutNanos/1000000, (int) (timeoutNanos%1000000));
            }
        }
    }

    /**
     * extracts next token
     * blocks when the buffer is empty
     * @return  token
     */
    public T take() throws InterruptedException {
        synchronized(inp)  {
            for (;;) {
                if (inp.isCompleted()) {
                    throw new CompletionException(inp.getCompletionException());
                }
                T res = inp.poll();
                if (res != null) {
                    return res;
                }
                inp.wait();
            }
        }
    }

    public void onComplete() {
        inp.onComplete();
    }

    public void onError(Throwable ex) {
        inp.onError(ex);
    }

    @Override
    protected void fire() {
        run();
    }

    protected void whenComplete(Throwable completionException) {
        out._onComplete(completionException);
    }

    @Override
    protected void runAction() throws Throwable {
        if (inp.isCompleted()) {
            complete(inp.getCompletionException());
        } else {
            T token = inp.remove();
            out.onNext(token);
        }
    }
}
