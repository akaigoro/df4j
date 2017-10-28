package org.df4j.core;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Created by Jim on 02-Jun-17.
 */
public class Transition {
    protected static final int CONTROL_BIT = 1;

    /**
     * main scale of bits, one bit per pin
     * when pinBits==0, transition fires
     */
    private AtomicInteger pinBits = new AtomicInteger();
    private int pinCount = 1; // control bit allocated

    /**
     * the head and the tail of the list of Pins
     */
    private Pin head, tail;

    protected final AtomicReference<Executor> executor = new AtomicReference<>();

    /**
     * assigns Executor
     */
    public Executor setExecutor(Executor exec) {
        Executor res = this.executor.getAndUpdate((prev)->exec);
        return res;
    }

    protected Executor getExecutor() {
        Executor exec = executor.get();
        return exec;
    }

    protected Executor getExecutorNotNull() {
        Executor exec = executor.get();
        if (exec == null) {
            exec = executor.updateAndGet((prev)->prev==null?ForkJoinPool.commonPool():prev);
        }
        return exec;
    }

    int registerPin(Pin pin) {
        if (pinCount == 32) {
            throw new IllegalStateException("only 32 transition could be created");
        }
        int pinBit = 1 << (pinCount++); // assign next pin number
        if (head == null) {
            head = pin;
        } else {
            tail.next = pin;
        }
        tail = pin;
        return pinBit;
    }

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
     * @return true if all transition ready
     */
    protected boolean _turnOn(int pinBit) {
        int next = pinBits.updateAndGet(pinBits -> {
            pinBits = pinBits & ~pinBit;
            if (pinBits == 0) {
                pinBits = CONTROL_BIT;
            }
            return pinBits;
        });
        return next == CONTROL_BIT;
    }

    /**
     * @return true if all data transition are ready
     */
    protected synchronized boolean consumeTokens() {
        for (Pin pin1 = head; pin1 != null; pin1 = pin1.next) {
            pin1._purge();
        }
        int next = pinBits.updateAndGet(pinBits -> pinBits == CONTROL_BIT ? CONTROL_BIT : pinBits & ~CONTROL_BIT);
        return next == CONTROL_BIT;
    }

    /**
     * Basic place for for places for input tokens.
     * Asynchronous version of binary semaphore.
     */
    public class Pin  {

        private final int pinBit; // distinct for all other transition of the node
        protected Pin next = null; // link to next pin

        protected Pin() {
            pinBit = registerPin(this);
            turnOff(); // mark this pin as blocked, to prevent premature firing.
        }

        /** unlock pin by setting it to 0
         * @return true if transition fired emitting control token
         */
        public boolean turnOn() {
            return _turnOn(pinBit);
        }

        /**
         * lock pin by setting it to 1
         * called when a token is consumed and the pin become empty
         */
        public void turnOff() {
            _turnOff(pinBit);
        }

        /**
         * Executed after token processing (method act). Cleans reference to
         * value, if any. Signals to set state to off if no more tokens are in
         * the place. Should return quickly, as is called from the actor's
         * synchronized block.
         */
        protected void _purge() {
            turnOff();
        }
    }

}
