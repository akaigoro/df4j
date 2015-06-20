package org.df4j.core.actor;

// ============== stuff for extending Pin from another package without  showing up lock
public abstract class PinBase<T> extends Pin {

    public PinBase(Actor actor) {
        super(actor);
    }

    /**
     * @return true if Pin should be turned on
     */
    protected boolean turnedOn(T token) {
        return true;
    }

    /**
     * @return true if Pin should be turned on
     */
    protected boolean turnedOn(long value) {
        return true;
    }

    protected final void checkOn(T token) {
        boolean doFire;
        gate.lock.lock();
        try {
            if (turnedOn(token)) {
                doFire = turnOn();
            } else {
                return;
            }
        } finally {
            gate.lock.unlock();
        }
        if (doFire) {
            gate.fire();
        }
    }

    protected final void checkOn(long value) {
        boolean doFire;
        gate.lock.lock();
        try {
            if (turnedOn(value)) {
                doFire = turnOn();
            } else {
                return;
            }
        } finally {
            gate.lock.unlock();
        }
        if (doFire) {
            gate.fire();
        }
    }
}