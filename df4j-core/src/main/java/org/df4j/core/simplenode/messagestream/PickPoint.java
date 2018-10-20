package org.df4j.core.simplenode.messagestream;

import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.boundconnector.messagescalar.SimpleSubscription;
import org.df4j.core.boundconnector.messagestream.StreamSubscriber;
import org.df4j.core.simplenode.messagescalar.SubscriberPromise;
import org.df4j.core.tasknode.messagestream.StreamCompletedException;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *  An asynchronous analogue of BlockingQueue
 *  (only on output end, while from the input side it does not block)
 *
 * @param <T> the type of the values passed through this token container
 */
public class PickPoint<T> extends ArrayDeque<T> implements StreamSubscriber<T>, ScalarPublisher<T>, BlockingQueue<T> {
    private boolean completed = false;
	/** place for demands */
	private Queue<ScalarSubscriber<? super T>> requests = new ArrayDeque<>();

	private SimpleSubscription subscription;

    public synchronized boolean isCompleted() {
        return completed;
    }

    public void onSubscribe(SimpleSubscription subscription){
        this.subscription = subscription;
    }

    @Override
	public synchronized void post(T token) {
        if (completed) {
            throw new IllegalStateException();
        }
	    if (requests.isEmpty()) {
	        super.add(token);
        } else {
	        requests.poll().complete(token);
        }
	}

	@Override
	public synchronized void complete() {
        if (completed) {
            return;
        }
        completed = true;
        for (ScalarSubscriber<? super T> subscriber: requests) {
            subscriber.completeExceptionally(new StreamCompletedException());
        }
        requests = null;
	}

	@Override
	public <S extends ScalarSubscriber<? super T>> S subscribe(S subscriber) {
        if (completed) {
            throw new IllegalStateException();
        }
		if (super.isEmpty()) {
			requests.add(subscriber);
		} else {
			subscriber.complete(super.poll());
		}
        return subscriber;
	}

    /**====================== implementation of synchronous BlockingQueu interface  ====================*/

    @Override
    public boolean add(T t) {
        post(t);
        return true;
    }

    @Override
    public boolean offer(T t) {
        post(t);
        return true;
    }

    @Override
    public void put(T t) throws InterruptedException {
        post(t);
    }

    @Override
    public synchronized boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
        post(t);
        return true;
    }

    @Override
    public T take() throws InterruptedException {
        synchronized(this) {
            if (!super.isEmpty() && requests.isEmpty()) {
                return super.remove();
            }
        }
        SubscriberPromise<T> future = new SubscriberPromise<>();
        subscribe(future);
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        synchronized(this) {
            if (!super.isEmpty() && requests.isEmpty()) {
                return super.remove();
            }
        }
        SubscriberPromise<T> future = new SubscriberPromise<>();
        subscribe(future);
        try {
            return future.get(timeout, unit);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized int remainingCapacity() {
        return 1;
    }

    @Override
    public synchronized int drainTo(Collection<? super T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized int drainTo(Collection<? super T> c, int maxElements) {
        throw new UnsupportedOperationException();
    }

    static class ThreadSupscriber {

    }
}
