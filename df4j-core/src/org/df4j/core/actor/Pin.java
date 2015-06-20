package org.df4j.core.actor;

//------------------------------------
/**
 * Basic place for input tokens. Initial state should be empty, to prevent
 * premature firing.
 */
abstract class Pin {
    Actor.SyncTask gate;
    Pin next = null; // link to list
    final int pinBit; // distinct for all other pins of the node

    public Pin(Actor actor) {
        this.gate = actor.task;
        gate.lock.lock();
        try {
            if (gate.pinCount == 32) {
                throw new IllegalStateException(
                        "only 32 pins could be created");
            }
            pinBit = 1 << gate.pinCount;
            turnOff();
            gate.pinCount++;
            // register itself in the pin list
            if (gate.head == null) {
                gate.head = this;
                return;
            }
            Pin prev = gate.head;
            while (prev.next != null) {
                prev = prev.next;
            }
            prev.next = this;
        } finally {
            gate.lock.unlock();
        }
    }

    /**
     * sets pin's bit on and fires task if all pins are on
     * 
     * @return true if actor became ready and must be fired
     */
    final boolean turnOn() {
        // System.out.print("turnOn "+fired+" "+allReady());
        gate.pinOn(pinBit);
        if (gate.allReady()) {
            gate._lockFire(); // to prevent multiple concurrent firings
            // System.out.println(" => true");
            return true;
        } else {
            // System.out.println(" => false");
            return false;
        }
    }

    /**
     * sets pin's bit off
     */
    protected final void turnOff() {
        // System.out.println("turnOff");
        gate.pinOff(pinBit);
    }

    /**
     * Executed after token processing (method act). Cleans reference to
     * value, if any. Signals to set state to off if no more tokens are in
     * the place. Should return quickly, as is called from the actor's
     * synchronized block. Default implementation does nothing.
     * 
     * @return true if Pin should remain on
     */
    protected boolean consume() {
        return true;// do not turn off
    }
}