package org.df4j.core;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Created by Jim on 02-Jun-17.
 */
public class Transition {
    protected static final int CONTROL_BIT = 1;

    private Actor actor;
    private Executor exec;
    private int pinBits = 0;
    private int pinCount = 1; // control bit allocated

    /**
     * the head and the tail of the list of Transition
     */
    private Actor.Pin head, tail;

    protected final AtomicReference<Executor> executor = new AtomicReference<>();

    public Transition(Actor actor) {
        this.actor = actor;
    }

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

    synchronized int registerPin(Actor.Pin pin) {
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
    protected synchronized void _turnOff(int pinBit) {
        pinBits |= pinBit;
    }

    /**
     * turns pinBit on, i.e. to 0
     * if pin scale makes all zeros, blocks control pin
     *
     * @param pinBit
     * @return true if all transition ready
     */
    protected synchronized boolean _turnOn(int pinBit) {
        int next = pinBits & ~pinBit;
        boolean res = (next == 0);
        if (res) {
            pinBits = CONTROL_BIT;
        } else {
            pinBits = next;
        }
        return res;
    }

    /**
     * @return true if all data transition are ready
     */
    protected synchronized boolean consumeTokens() {
        for (Actor.Pin pin1 = head; pin1 != null; pin1 = pin1.next) {
            pin1._purge();
        }
        if (pinBits == 1) {
            return true;
        } else {
            _turnOn(CONTROL_BIT);
            return false;
        }
    }
}
