package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.protocol.Disposable;
import org.df4j.protocol.Single;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one message.
 * After the message is received, this port stays ready forever.
 *
 * @param <T> type of accepted tokens.
 */
public class InpSingle<T> extends BasicBlock.Port implements Single.Observer<T> {
    protected T value;
    protected volatile boolean completed = false;
    private Throwable completionException = null;

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public InpSingle(BasicBlock parent) {
        parent.super(false);
    }

    public boolean isCompleted() {
        plock.lock();
        try {
            return completed;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onSubscribe(Disposable subscription) {

    }

    public Throwable getCompletionException() {
        return completionException;
    }

    public T current() {
        plock.lock();
        try {
            return value;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public  void onSuccess(T message) {
        plock.lock();
        try {
            if (completed) {
                return;
            }
            this.completed = true;
            this.value = message;
            unblock();
        } finally {
            plock.unlock();
        }
    }

    @Override
    public  void onError(Throwable throwable) {
        plock.lock();
        try {
            if (completed) {
                return;
            }
            this.completed = true;
            this.completionException = throwable;
            unblock();
        } finally {
            plock.unlock();
        }
    }
}
