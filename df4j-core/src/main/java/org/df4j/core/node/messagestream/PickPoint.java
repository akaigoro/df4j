package org.df4j.core.node.messagestream;

import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.connector.messagestream.StreamCollector;

import java.util.ArrayDeque;
import java.util.Queue;

public class PickPoint<T> implements StreamCollector<T>, ScalarPublisher<T> {
    private boolean completed = false;
	/** place for demands */
	private Queue<ScalarSubscriber<? super T>> requests = new ArrayDeque<>();
	/** place for resources */
	private Queue<T> resources = new ArrayDeque<>();

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

}
