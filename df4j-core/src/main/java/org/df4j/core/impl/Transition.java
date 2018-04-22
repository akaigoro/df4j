package org.df4j.core.impl;

import org.df4j.core.spi.messagescalar.Port;
import org.df4j.core.spi.messagestream.StreamPort;

import java.io.Closeable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Jim on 02-Jun-17.
 */
public abstract class Transition {

    /**
     * main scale of bits, one bit per pin
     * when pinBits becomes 0, transition fires
     */
    private AtomicLong pinBits = new AtomicLong();
    private int pinCount = 0;
    /** the list of all Pins */
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
        long res =  pinBits.updateAndGet(pinBits -> {
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
     * invoked when all transition transition are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    protected void fire() {
        run();
    }

    public void run() {
        try {
            act();
        } catch (Throwable e) {
            System.err.println("Error in actor " + getClass().getName());
            e.printStackTrace();
        }
    }

    /**
     * reads extracted tokens from places and performs specific calculations
     *
     * @throws Exception
     */
    protected abstract void act() throws Exception;

    /*==================================================*/

    /**
     * by defaukt, initially in blocing state
     */
    public class Pin {

        private final int pinBit; // distinct for all other transition of the node

        public Pin() {
            this(true);
        }

        protected Pin(boolean blocked) {
            if (pinCount == 64) {
                throw new IllegalStateException("only 64 transition could be created");
            }
            pinBit = 1 << (pinCount++); // assign next pin number
            pins.add(this);
            if (blocked) {
                turnOff();
            }
        }


        public void turnOn() {
            boolean on = Transition.this._turnOn(pinBit);
            if (on) {
                fire();
            }
        }

        /**
         * Executed after token processing (method act). Cleans reference to
         * value, if any. Signals to set state to off if no more tokens are in
         * the place. Should return quickly, as is called from the actor's
         * synchronized block.
         */
        protected void purge() {
        }

        /**
         * lock pin by setting it to 1
         * called when a token is consumed and the pin become empty
         */
        protected void turnOff() {
            _turnOff(pinBit);
        }

    }

    // ========= classes

    /**
     * Counting semaphore
     * holds token counter without data.
     * counter can be negative.
     */
    public class Semafor extends Pin implements Closeable {
        private volatile boolean closed = false;
        private long count = 0;

        public Semafor() {
        }

        public boolean isClosed() {
            return closed;
        }

        public long getCount() {
            return count;
        }

        @Override
        public synchronized void close() {
            closed = true;
            count = 0;
            turnOff(); // and cannot be turned on
        }

        /** increments resource counter by delta */
        public synchronized void release(long delta) {
            if (closed) {
                throw new IllegalStateException("closed already");
            }
            if (delta < 0) {
                throw new IllegalArgumentException("resource counter delta must be >= 0");
            }
            long prev = count;
            count+= delta;
            if (prev <= 0 && count > 0 ) {
                turnOn();
            }
        }

        /** increments resource counter by delta */
        protected synchronized void aquire(long delta) {
            if (delta < 0) {
                throw  new IllegalArgumentException("resource counter delta must be >= 0");
            }
            long prev = count;
            count -= delta;
            if (prev > 0 && count <= 0 ) {
                turnOff();
            }
        }

        @Override
        protected synchronized void purge() {
            aquire(1);
        }
    }

    //=============================== scalars

    /*******************************************************
     * Token storage with standard Port<T> interface. It has place for only one
     * token, which is never consumed.
     *
     * @param <T>
     *     type of accepted tokens.
     */
    public class ConstInput<T> extends Pin implements Port<T> {

        /** extracted token */
        public T value = null;

        public T get() {
            return value;
        }

        /**
         *  @throws NullPointerException
         *  @throws IllegalStateException
         */
        @Override
        public synchronized void post(T token) {
            if (token == null) {
                throw new IllegalArgumentException();
            }
            if (value != null) {
                throw new IllegalStateException("token set already");
            }
            value = token;
            turnOn();
        }

        /**
         * pin bit remains ready
         */
        @Override
        protected void purge() {
        }
    }

    /**
     * Token storage with standard Port<T> interface.
     * It has place for only one token.
     *
     * @param <T>
     *            type of accepted tokens.
     */
    public class Input<T> extends ConstInput<T> implements Port<T> {
        protected boolean pushback = false; // if true, do not consume

        // ===================== backend

        protected void pushback() {
            if (pushback) {
                throw new IllegalStateException();
            }
            pushback = true;
        }

        protected synchronized void pushback(T value) {
            if (pushback) {
                throw new IllegalStateException();
            }
            pushback = true;
            this.value = value;
        }

        @Override
        protected synchronized void purge() {
            if (pushback) {
                pushback = false;
                // value remains the same, the pin remains turned on
            } else {
                value = null;
                turnOff();
            }
        }
    }

    //=============================== streams

    /*******************************************************
     * A Queue of tokens of type <T>
     *
     * @param <T>
     */
    public class StreamInput<T> extends Input<T> implements StreamPort<T> {
        protected Deque<T> queue;
        protected boolean closeRequested = false;

        public StreamInput () {
            this.queue = new ArrayDeque<T>();
        }

        public StreamInput (int capacity) {
            this.queue = new ArrayDeque<T>(capacity);
        }

        public StreamInput(Deque<T> queue) {
            this.queue = queue;
        }

        protected int size() {
            return queue.size();
        }

        @Override
        public synchronized void post(T token) {
            if (token == null) {
                throw new NullPointerException();
            }
            if (closeRequested) {
                throw new IllegalStateException("closed already");
            }
            if (value == null) {
                value = token;
                turnOn();
            } else {
                queue.add(token);
            }
        }

        /**
         * Signals the end of the stream. Turns this pin on. Removed value is
         * null (null cannot be send with StreamInput.add(message)).
         */
        @Override
        public synchronized void close() {
            if (closeRequested) {
                return;
            }
            closeRequested = true;
            if (value == null) {
                turnOn();
            }
        }

        @Override
        protected void pushback() {
            if (pushback) {
                throw new IllegalStateException();
            }
            pushback = true;
        }

        @Override
        protected synchronized void pushback(T value) {
            if (value == null) {
                throw new IllegalArgumentException();
            }
            if (!pushback) {
                pushback = true;
            } else {
                if (this.value == null) {
                    throw new IllegalStateException();
                }
                queue.addFirst(this.value);
                this.value = value;
            }
        }

        /**
         * attempt to take next token from the input queue
         *
         * @return true if next token is available, or if stream is closed false
         *         if input queue is empty
         */
        public boolean moveNext() {
            synchronized(this) {
                if (pushback) {
                    pushback = false;
                    return true;
                }
                boolean wasNotNull = (value != null);
                T newValue = queue.poll();
                if (newValue != null) {
                    value = newValue;
                    return true;
                } else if (closeRequested) {
                    value = null;
                    return wasNotNull;// after close, return true once, then
                    // false
                } else {
                    return false;
                }
            }
        }

        @Override
        protected synchronized void purge() {
            if (pushback) {
                pushback = false;
                return; // value remains the same, the pin remains turned on
            }
            boolean wasNull = (value == null);
            value = queue.poll();
            if (value != null) {
                return; // the pin remains turned on
            }
            // no more tokens; check closing
            if (wasNull || !closeRequested) {
                turnOff();
            }
            // else process closing: value is null, the pin remains turned on
        }

        public synchronized boolean  isClosed() {
            return closeRequested && (value == null);
        }
    }

}
