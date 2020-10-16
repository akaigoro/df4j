package org.df4j.core.communicator;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.port.InpChannel;
import org.df4j.core.port.OutFlow;
import org.df4j.protocol.Flow;
import org.df4j.protocol.ReverseFlow;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *  Partially implemented {@link BlockingQueue} augmented with asynchronous interfaces to save and extract messages,
 *  and also interfaces to pass completion signal as required by {@link Flow}.
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

    public AsyncArrayBlockingQueue(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException();
        }
        inp = new InpChannel<>(this, capacity>1? capacity-1:1);
        out = new OutFlow<>(this, 1);
        start();
    }

    public AsyncArrayBlockingQueue() {
        this(32);
    }

    @Override
    public void feedFrom(ReverseFlow.Producer<T> producer) {
        inp.feedFrom(producer);
    }

    @Override
    public void feedFrom(Publisher<T> publisher) {
        inp.feedFrom(publisher);
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        out.subscribe(s);
    }

    public boolean offer(T token) {
        return inp.offer(token);
    }

    public boolean offer(T token, int timeout, TimeUnit unit) throws InterruptedException {
        return inp.offer(token, timeout, unit);
    }

    public void add(T token) {
        inp.add(token);
    }

    public void put(T token) throws InterruptedException {
        inp.put(token);
    }

    public int size() {
        return inp.size()+out.size();
    }

    public T remove() {
        T res = out.poll();
        if (res == null) {
            throw new IllegalStateException();
        }
        return res;
    }

    public T poll() {
        return out.poll();
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return out.poll(timeout, unit);
    }

    public T take() throws InterruptedException {
        return out.take();
    }

    @Override
    public void complete() {
        inp.onComplete();
    }

    @Override
    public void completeExceptionally(Throwable ex) {
        inp.onError(ex);
    }

    @Override
    protected void fire() {
        run();
    }

    @Override
    protected void runAction() throws Throwable {
        if (inp.isCompleted()) {
            out._onComplete(inp.getCompletionException());
            super.complete();
        } else {
            T token = inp.remove();
            out.onNext(token);
        }
    }
}
