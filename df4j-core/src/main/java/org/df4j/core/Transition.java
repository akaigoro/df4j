package org.df4j.core;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Created by Jim on 02-Jun-17.
 */
public class Transition implements Runnable {
    protected static final int CONTROL_BIT = 1;

    private int pinBits = 0;

    private int pinCount = 1; // control bit allocated

    /**
     * the head and the tail of the list of Transition
     */
    private Actor.Pin head, tail;

    private Actor actor;

    protected final AtomicReference<Executor> executor = new AtomicReference<>();

    public Transition(Actor actor) {
        this.actor = actor;
    }

    protected Executor getExecutor() {
        Executor exec = executor.get();
        if (exec != null) {
            return exec;
        }
        exec = Actor.getDefaultExecutor();
        if (exec == null) {
            exec = ForkJoinPool.commonPool();
        }
        executor.set(exec);
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

    public void forAll(Consumer<Actor.Pin> proc) {
        for (Actor.Pin pin = head; pin != null; pin = pin.next) {
            proc.accept(pin);
        }
    }

    /**
     * @return true if all data transition are ready
     */
    protected synchronized boolean consumeTokens() {
        forAll(pin -> pin._purge());
        if (pinBits == 1) {
            return true;
        } else {
            _turnOn(CONTROL_BIT);
            return false;
        }
    }

    /**
     * invoked when all transition transition are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    public void fire() {
        getExecutor().execute(this);
    }

    /**
     * loops while all transition are ready
     */
    @Override
    public void run() {
        try {
            do {
                actor.act();
            } while (consumeTokens());
        } catch (Throwable e) {
            System.err.println("Actor.act():" + e);
            e.printStackTrace();
        }
    }

    /**
     * assigns Executor
     */
    public Executor setExecutor(Executor exec) {
        Executor res = this.executor.get();
        this.executor.set(exec);
        return res;
    }
}
