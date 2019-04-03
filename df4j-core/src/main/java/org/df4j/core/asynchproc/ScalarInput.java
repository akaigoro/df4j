package org.df4j.core.asynchproc;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one token.
 *
 * @param <T> type of accepted tokens.
 */
public class ScalarInput<T> extends AsyncProc<T>.ConstInput<T> {
    protected AsyncProc task;
    protected boolean pushback = false; // if true, do not consume

    public ScalarInput(AsyncProc task) {
        task.super();
        this.task = task;
    }

    // ===================== backend

    public boolean hasNext() {
        return !isDone();
    }

    public T next() {
        if (exception != null) {
            throw new RuntimeException(exception);
        }
        if (current == null) {
            throw new IllegalStateException();
        }
        T res = current;
        if (pushback) {
            pushback = false;
            // value remains the same, the pin remains turned on
        } else {
            current = null;
            block();
        }
        return res;
    }
}
