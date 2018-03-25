package org.df4j.core;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jim on 02-Jun-17.
 */
public class Transition {

    /**
     * main scale of bits, one bit per pin
     * when pinBits becomes 0, transition fires
     */
    private AtomicInteger pinBits = new AtomicInteger();
    private int pinCount = 0;

    /**
     * the list of all Pins
     */
    private ArrayList<Pin> pins = new ArrayList<>(4);

    /**
     * locks pin by setting it to 1
     * called when a token is consumed and the pin become empty
     *
     * @param pinBit
     */
    protected void _turnOff(int pinBit) {
        pinBits.updateAndGet(pinBits -> pinBits | pinBit);
    }

    /**
     * turns pinBit on, i.e. to 0
     * if pin scale makes all zeros, blocks control pin
     *
     * @param pinBit
     * @return true if all transition become ready
     */
    protected boolean _turnOn(int pinBit) {
        int res =  pinBits.updateAndGet(pinBits -> {
            if (pinBits == 0) {
                return 1;
            }
            pinBits = pinBits & ~pinBit;
            return pinBits;
        });
        return res == 0;
    }

    protected synchronized void consumeTokens() {
        for (int k=0; k<pins.size(); k++) {
            Pin pin = pins.get(k);
            pin.purge();
        }
    }

    /**
     * Basic place for for places for input tokens.
     * Asynchronous version of binary semaphore.
     */
    public abstract class Pin  {

        private final int pinBit; // distinct for all other transition of the node

        protected Pin() {
            if (pinCount == 32) {
                throw new IllegalStateException("only 32 transition could be created");
            }
            pinBit = 1 << (pinCount++); // assign next pin number
            pins.add(this);
            turnOff();
        }

        /** unlock pin by setting it to 0
         * @return true if transition fired emitting control token
         */
        protected boolean turnOn() {
            return _turnOn(pinBit);
        }

        /**
         * lock pin by setting it to 1
         * called when a token is consumed and the pin become empty
         */
        protected void turnOff() {
            _turnOff(pinBit);
        }

        /**
         * Executed after token processing (method act). Cleans reference to
         * value, if any. Signals to set state to off if no more tokens are in
         * the place. Should return quickly, as is called from the actor's
         * synchronized block.
         */
        protected abstract  void purge();
    }
}
