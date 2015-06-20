package org.df4j.core.actor;

import org.df4j.core.Port;

/**
 * Token storage with standard Port<T> interface. It has place for only one
 * token, which is not consumed.
 * 
 * @param <T>
 *            type of accepted tokens.
 */
public class ConstInput<T> extends Pin implements Port<T> {
    public ConstInput(Actor actor) {
        super(actor);
    }
    
    /** extracted token */
    T value = null;

    @Override
    public void post(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        boolean doFire;
        gate.lock.lock();
        try {
            if (value != null) {
                throw new IllegalStateException("token set already");
            }
            value = token;
            doFire = turnOn();
        } finally {
            gate.lock.unlock();
        }
        if (doFire) {
            gate.fire();
        }
    }

    public T get() {
        return value;
    }

    // ===================== backend

    /**
     * removes token from the storage
     * 
     * @return removed token
     */
    protected T poll() {
        return null;
    }
}