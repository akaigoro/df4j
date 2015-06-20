package org.df4j.core.actor;

/**
 * holds token counter without data counter can be negative
 */
public class Semafor extends Pin {
    private int count;

    public Semafor(Actor actor) {
        super(actor);
        this.count = 0;
    }

    public Semafor(Actor actor, int count) {
        super(actor);
        if (count > 0) {
            throw new IllegalArgumentException(
                    "initial counter cannot be positive");
        }
        this.count = count;
    }

    /** increments resource counter by 1 */
    public void up() {
        boolean doFire;
        gate.lock.lock();
        try {
            count++;
            if (count != 1) {
                return;
            }
            doFire = turnOn();
        } finally {
            gate.lock.unlock();
        }
        if (doFire) {
            gate.fire();
        }
    }

    /** increments resource counter by delta */
    public void up(int delta) {
        boolean doFire;
        gate.lock.lock();
        try {
            boolean wasOff = (count <= 0);
            count += delta;
            boolean isOff = (count <= 0);
            if (wasOff == isOff) {
                return;
            }
            if (isOff) {
                turnOff();
                return;
            }
            doFire = turnOn();
        } finally {
            gate.lock.unlock();
        }
        if (doFire) {
            gate.fire();
        }
    }

    /** decrements resource counter */
    public void down() {
        gate.lock.lock();
        try {
            consume();
        } finally {
            gate.lock.unlock();
        }
    }

    @Override
    protected boolean consume() {
        return --count > 0;
    }
}