package org.df4j.core.actor;

import org.df4j.core.Port;

/**
 * Token storage with standard Port<T> interface. By default, it has place
 * for only one token.
 * 
 * @param <T>
 *            type of accepted tokens.
 */
public class Input<T> extends ConstInput<T> implements Port<T> {
    public Input(Actor actor) {
        super(actor);
    }

    boolean pushback = false; // if true, do not consume

    public T get() {
        return value;
    }

    // ===================== backend

    public void pushback() {
        if (pushback) {
            throw new IllegalStateException();
        }
        pushback = true;
    }

    protected void pushback(T value) {
        if (pushback) {
            throw new IllegalStateException();
        }
        pushback = true;
        this.value = value;
    }

    @Override
    protected boolean consume() {
        if (pushback) {
            pushback = false;
            // value remains the same, the pin remains turned on
            return true;
        }
        // check closing
        if ((value == null)) {
            return false;
        } else {
            // else make one more round with value==null
            value = null;
            return true;
        }
    }
}