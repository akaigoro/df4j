package org.df4j.core.simplenode.messagestream;

import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.reactivestreams.Subscription;
import org.df4j.core.boundconnector.messagestream.StreamSubscriber;
import org.df4j.core.simplenode.messagescalar.CompletablePromise;
import org.df4j.core.tasknode.messagestream.StreamCompletedException;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *  An asynchronous analogue of BlockingQueue
 *  (only on output end, while from the input side it does not block)
 *
 * @param <T> the type of the values passed through this token container
 */
public class PickPoint<T> implements ScalarPublisher<T>, StreamSubscriber<T> {
    protected ArrayDeque<T> resources = new ArrayDeque<>();
    protected boolean completed = false;
	/** place for demands */
	private Queue<ScalarSubscriber<? super T>> requests = new ArrayDeque<>();

    public synchronized boolean isCompleted() {
        return completed;
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
	public synchronized void onComplete() {
        if (completed) {
            return;
        }
        completed = true;
        for (ScalarSubscriber<? super T> subscriber: requests) {
            subscriber.postFailure(new StreamCompletedException());
        }
        requests = null;
	}

	@Override
    public Subscription subscribe(ScalarSubscriber<T> subscriber) {
        if (completed) {
            throw new IllegalStateException();
        }
		if (resources.isEmpty()) {
			requests.add(subscriber);
		} else {
			subscriber.post(resources.poll());
		}
		return null;
	}

    public T take() throws InterruptedException {
        synchronized(this) {
            if (!resources.isEmpty() && requests.isEmpty()) {
                return resources.remove();
            }
        }
        CompletablePromise<T> future = new CompletablePromise<>();
        subscribe(future);
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        synchronized(this) {
            if (!resources.isEmpty() && requests.isEmpty()) {
                return resources.remove();
            }
        }
        CompletablePromise<T> future = new CompletablePromise<>();
        subscribe(future);
        try {
            return future.get(timeout, unit);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
