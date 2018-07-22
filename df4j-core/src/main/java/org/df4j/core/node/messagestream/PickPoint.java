package org.df4j.core.node.messagestream;

import org.df4j.core.node.messagescalar.SubscriberPromise;
import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.connector.messagescalar.SimpleSubscription;
import org.df4j.core.connector.messagestream.StreamSubscriber;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * An asynchronous analogue of BlockingQueue
 *  (only on output end, while from the input side it does not block)
 * @param <T>
 */
public class PickPoint<T> implements StreamSubscriber<T>, ScalarPublisher<T>, BlockingQueue<T> {
    private boolean completed = false;
	/** place for demands */
	private Queue<ScalarSubscriber<? super T>> requests = new ArrayDeque<>();
	/** place for resources */
	private Queue<T> resources = new ArrayDeque<>();

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
	        resources.add(token);
        } else {
	        requests.poll().post(token);
        }
	}

	@Override
	public synchronized void complete() {
        if (completed) {
            return;
        }
        completed = true;
        resources = null;
        for (ScalarSubscriber<? super T> subscriber: requests) {
            subscriber.postFailure(new StreamCompletedException());
        }
        requests = null;
	}

	@Override
	public <S extends ScalarSubscriber<? super T>> S subscribe(S subscriber) {
        if (completed) {
            throw new IllegalStateException();
        }
		if (resources.isEmpty()) {
			requests.add(subscriber);
		} else {
			subscriber.post(resources.poll());
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
    public synchronized T remove() {
        return resources.remove();
    }

    @Override
    public synchronized T poll() {
        return resources.poll();
    }

    @Override
    public synchronized T element() {
        return resources.element();
    }

    @Override
    public synchronized T peek() {
        return resources.peek();
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
            if (!resources.isEmpty() && requests.isEmpty()) {
                return resources.remove();
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
            if (!resources.isEmpty() && requests.isEmpty()) {
                return resources.remove();
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
    public synchronized boolean remove(Object o) {
        return resources.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return resources.containsAll(c);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends T> c) {
        return resources.addAll(c);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        return resources.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return resources.retainAll(c);
    }

    @Override
    public synchronized void clear() {
        resources.clear();
    }

    @Override
    public synchronized int size() {
        return resources.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return resources.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return resources.contains(o);
    }

    @Override
    public synchronized Iterator<T> iterator() {
        return resources.iterator();
    }

    @Override
    public synchronized Object[] toArray() {
        return resources.toArray();
    }

    @Override
    public synchronized <T1> T1[] toArray(T1[] a) {
        return resources.toArray(a);
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
