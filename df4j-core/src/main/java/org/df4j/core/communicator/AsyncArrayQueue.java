package org.df4j.core.communicator;

import org.df4j.core.protocol.MessageChannel;
import org.df4j.core.protocol.MessageStream;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 *  An asynchronous analogue of ArrayBlockingQueue
 *
 * @param <T> the type of the values passed through this token container
 */
public class AsyncArrayQueue<T> extends AbstractQueue<T> implements BlockingQueue<T>, MessageChannel.Consumer<T>, MessageStream.Publisher<T> {
    protected final int capacity;
    protected ArrayDeque<T> tokens;
    protected Queue<MessageChannel.Producer<T>> producers = new LinkedList<>();
    protected Queue<MessageStream.Subscriber<T>> subscribers = new LinkedList<>();
    protected Throwable completionException;
    protected volatile boolean completed;

    public AsyncArrayQueue(int capacity) {
        this.capacity = capacity;
        tokens = new ArrayDeque<>(capacity);
    }

    @Override
    public void offer(MessageChannel.Producer<T> p) {
        if (completed) {
            return;
        }
        T value = p.remove();
        handleValue:
        if (value != null) {
            MessageStream.Subscriber<T> sub;
            synchronized (this) {
                if (!subscribers.isEmpty()) {
                    sub = subscribers.remove();
                } else if (tokens.size() < capacity) {
                    super.add(value);
                    break handleValue;
                } else {
                    producers.add(p);
                    break handleValue;
                }
            }
            sub.onNext(value);
        }
        if (p.isCompleted()) {
            onError(p.getCompletionException());
        }
    }

    public void onError(Throwable completionException) {
        Queue<MessageStream.Subscriber<T>> subs;
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
            this.completionException = completionException;
            notifyAll();
            if (subscribers.isEmpty()) {
                return;
            }
            subs = subscribers;
            subscribers = null;
        }
        for (;;) {
            MessageStream.Subscriber<T> sub = subs.poll();
            if (sub == null) {
                break;
            }
            sub.onError(completionException);
        }
    }

    public void onComplete() {
        onError(null);
    }

    @Override
    public synchronized void cancel(MessageChannel.Producer<? super T> s) {
        producers.remove(s);
    }

    @Override
    public void subscribe(MessageStream.Subscriber<T> subscriber) {
        T token;
        synchronized (this) {
            token = tokens.poll();
            if (token == null) {
                if (completed) {
                    subscriber.onError(completionException);
                } else {
                    subscribers.add(subscriber);
                }
                return;
            }
            MessageChannel.Producer<T> producer = producers.poll();
            if (producer == null) {
                notifyAll();
            } else {
                if (producer.isCompleted()) {
                    if (!completed) {
                        completed = true;
                        completionException = producer.getCompletionException();
                    }
                } else {
                    T newT = producer.remove();
                    tokens.addLast(newT);
                }
            }
        }
        subscriber.onNext(token);
    }

    @Override
    public synchronized boolean unsubscribe(MessageStream.Subscriber<T> subscriber) {
        return subscribers.remove(subscriber);
    }

    /**
     *
     * @param token token to insert
     * @return true if token inserted
     */
    @Override
    public boolean offer(T token) {
        MessageStream.Subscriber<T> sub;
        synchronized (this) {
            if (subscribers.isEmpty()) {
                return tokens.offer(token);
            }
            sub = subscribers.remove();
        }
        sub.onNext(token);
        return true;
    }

    @Override
    public T poll() {
        return null;
    }

    @Override
    public T peek() {
        return null;
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
        MessageStream.Subscriber<T> sub;
        long millis = unit.toMillis(timeout);
        synchronized(this) {
            for (;;) {
                if (completed) {
                    return false;
                }
                if (!subscribers.isEmpty()) {
                    sub = subscribers.remove();
                    break;
                } else if (tokens.offer(token)) {
                    return true;
                }
                if (millis <= 0) {
                    return false;
                }
                long targetTime = System.currentTimeMillis() + millis;
                wait(millis);
                millis = targetTime - System.currentTimeMillis();
            }
        }
        sub.onNext(token);
        return true;
    }

    @Override
    public synchronized void put(T token) throws InterruptedException {
        MessageStream.Subscriber<T> sub;
        synchronized(this) {
            for (;;) {
                if (completed) {
                    return;
                }
                if (!subscribers.isEmpty()) {
                    sub = subscribers.remove();
                    break;
                } else if (tokens.offer(token)) {
                    notifyAll();
                    return;
                }
                wait();
            }
        }
        sub.onNext(token);
    }

    @Override
    public synchronized T take() throws InterruptedException {
        synchronized(this) {
            for (;;) {
                T token = tokens.poll();
                if (token != null) {
                    MessageChannel.Producer<T> prod = producers.poll();
                    if (prod != null) {
                        offer(prod);
                    } else {
                        notifyAll();
                    }
                    return token;
                } else if (completed) {
                    throw new CompletionException(completionException);
                }
                wait(100); //todo this is a workaround
            }
        }
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        MessageChannel.Producer<T> prod;
        long millis = unit.toMillis(timeout);
        synchronized(this) {
            for (;;) {
                if (completed) {
                    throw new CompletionException(completionException);
                }
                if (!producers.isEmpty()) {
                    prod = producers.remove();
                    break;
                }
                T token = tokens.poll();
                if (token != null) {
                    return token;
                }
                if (millis <= 0) {
                    return null;
                }
                long targetTime = System.currentTimeMillis() + millis;
                wait(millis);
                millis = targetTime - System.currentTimeMillis();
            }
        }
        return prod.remove();
    }

    @Override
    public synchronized int remainingCapacity() {
        return capacity - tokens.size();
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
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return 0;
    }
}
