package org.df4j.core.port;

import org.df4j.core.actor.BasicBlock;
import org.df4j.core.communicator.CompletableObservers;
import org.df4j.core.protocol.MessageStream;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Publisher acting like a server
 * unblocked initially
 */
public class OutMessage<T> extends BasicBlock.Port implements MessageStream.Publisher<T> {
    CompletableObservers<MessageStream.Subscriber> subscribers = new CompletableObservers<>();
    protected volatile T value;

    public OutMessage(BasicBlock parent) {
        parent.super(true);
    }

    @Override
    public void subscribe(MessageStream.Subscriber<T> s) {
        T v;
        boolean finished;
        synchronized(this) {
            if (isReady()) {
                subscribers.subscribe(s);
                return;
            }
            v = value;
            value = null;
            finished = unblock();
        }
        s.onNext(v);
        if (finished) {
            return;
        }
        decBlocking();
    }

    @Override
    public boolean unsubscribe(MessageStream.Subscriber<T> s) {
        return subscribers.unsubscribe(s);
    }

    public void onNext(T t) {
        MessageStream.Subscriber<? super T> s;
        synchronized(this) {
            if (subscribers.isCompleted()) { // this is how CompletableFuture#completeExceptionally works
                return;
            }
            if (!super.isReady()) {
                throw new IllegalStateException();
            }
            s = subscribers.poll();
            if (s == null) {
                value = t;
                super.block();
                notifyAll();
                return;
            }
        }
        s.onNext(t);
    }

    public synchronized void onError(Throwable t) {
        if (subscribers.isCompleted()) { // this is how CompletableFuture#completeExceptionally works
            return;
        }
        subscribers.onError(t);
    }

    public void onComplete() {
        subscribers.onComplete();
    }

    public synchronized T take() {
        while (!subscribers.isCompleted()) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
        }
        if (subscribers.getCompletionException() == null) {
            return value;
        } else {
            throw new CompletionException(subscribers.getCompletionException());
        }
    }

    public synchronized T take(long timeout, TimeUnit unit) {
        if (value == null) {
            long millis = unit.toMillis(timeout);
            long targetTime = System.currentTimeMillis() + millis;
            for (;;) {
                if (subscribers.isCompleted()) {
                    if (subscribers.getCompletionException() != null) {
                        throw new CompletionException(subscribers.getCompletionException());
                    } else {
                        break;
                    }
                }
                if (millis <= 0) {
                    throw new CompletionException(new TimeoutException());
                }
                try {
                    wait(millis);
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
                if (value != null) {
                    break;
                }
                millis = targetTime - System.currentTimeMillis();
            }
        }
        T res = value;
        value = null;
        return res;
    }
}
