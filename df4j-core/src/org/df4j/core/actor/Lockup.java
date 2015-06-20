package org.df4j.core.actor;

/**
 * A lock is turned on or off permanently
 */
public class Lockup extends Pin {
    public Lockup(Actor actor) {
        super(actor);
    }

    public void on() {
        boolean doFire;
        gate.lock.lock();
        try {
            doFire = turnOn();
        } finally {
            gate.lock.unlock();
        }
        if (doFire) {
            gate.fire();
        }
    }

    public void off() {
        gate.lock.lock();
        try {
            turnOff();
        } finally {
            gate.lock.unlock();
        }
    }
}