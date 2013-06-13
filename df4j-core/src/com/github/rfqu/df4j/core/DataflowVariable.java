package com.github.rfqu.df4j.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * General dataflow node with several inputs and outputs.
 * Firing occur when all inputs are filled.
 * Typical use case is:
 *  - create 1 or more pins for inputs and/or outputs
 *  - redefine abstract method act()
 */
public abstract class DataflowVariable {
    private Lock lock = new ReentrantLock();
    private Throwable exc=null;
    private Pin head; // the head of the list of Pins
    private int pinCount=1; // fire bit allocated
    private int readyPins=0;  // mask with 0 for ready pins, 1 for blocked
    
    /** lock pin */
    protected final void pinOff(int pinBit) {
        readyPins |= pinBit;
    }

    /** unlock pin */
    protected final void pinOn(int pinBit) {
        readyPins &= ~pinBit;
    }

    protected final void fireLock() {
        pinOff(1);
    }

    protected final void fireUnlock() {
        pinOn(1);
    }

    protected final boolean isFired() {
        return (readyPins&1)==1;
    }

    /**
     * @return true if the actor has all its pins on and so is ready for execution
     */
    private final boolean allInputsReady() {
        return (readyPins|1)==1;
    }
    
    private final boolean allReady() {
        return readyPins==0;
    }
    
    public  void postFailure(Throwable exc) {
        boolean doFire;       
        lock.lock();
        try {
            if (this.exc!=null) {
                return; // only first failure is processed 
            }
            this.exc=exc;
            if (doFire=!isFired()) {
                fireLock();
            }
        } finally {
          lock.unlock();
        }
        if (doFire) {
            fire();
        }
    }

    /**
     * Processes combined input event synchronously.  
     * Override this method for asynchronous event processing,
     * using ActorTask.
     * 
     */
    protected void fire() {
        loopAct();
    }
    
    private final void loopAct() {
        execLoop:
        try {
            // the loop slightly unrolled to have only one
            // synchronized statement in the loop
            lock.lock();
            try {
                if (exc!=null) {
                    break execLoop; // fired remains true, preventing subsequent execution
                }
            } finally {
                lock.unlock();
            }
            act();
            for (;;) {
                lock.lock();
                try {
                    // consume tokens
                    for (Pin pin=head; pin!=null; pin=pin.next) {
                        pin.consume();
                    }
                    if (!allInputsReady()) {
                        fireUnlock(); // allow firing
                        return;
                    }
                    if (exc!=null) {
                        break execLoop; // fired remains true, preventing subsequent execution
                    }
                }
                finally {
                  lock.unlock();
                }
                act();
            }
        } catch (Throwable e) {
            exc=e;
        }
        try {
            handleException(exc);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    //========= backend
    
    /**
     * reads extracted tokens from places and performs specific calculations 
     */
    protected abstract void act();

    protected void handleException(Throwable exc) {
        System.err.println("DataflowNode.handleException:"+exc);
        exc.printStackTrace();
    }

    //====================== inner classes
    protected class ActorTask extends Task {
        
        public ActorTask() {
        }

        public ActorTask(Executor executor) {
            super(executor);
        }

        /** loops while all pins are ready
         */
        @Override
        public void run() {
            //System.out.println("ActorTask run");
            loopAct();
        }

    }

    /**
     * Basic place for input tokens.
     * Initial state should be empty, to prevent premature firing.
     */
    protected abstract class Pin {
        private Pin next; // link to list
        private final int pinBit; // distinct for all other pins of the node 

        protected Pin(){
            lock.lock();
            try {
                if (pinCount==32) {
                  throw new IllegalStateException("only 32 pins could be created");
                }
                pinBit = 1<<pinCount;
                turnOff();
                pinCount++;
                next=head; head=this; // register itself in the pin list
            } finally {
              lock.unlock();
            }
        }

        /**
         * sets pin's bit on and fires task if all pins are on
         *  @return true if actor became ready and must be fired
         */
        protected boolean turnOn() {
            //System.out.print("turnOn "+fired+" "+allReady());
            pinOn(pinBit);
            if (!allReady()) {
                //System.out.println(" => false");
                return false;
            }
            fireLock(); // to prevent multiple concurrent firings
            //System.out.println(" => true");
            return true;
        }

        /**
         * sets pin's bit off
         */
        protected void turnOff() {
            //System.out.println("turnOff");
            pinOff(pinBit);
        }
        
        /** Executed after token processing (method act).
         * Cleans reference to value, if any.
         * Sets state to off if no more tokens are in the place.
         * Should return quickly, as is called from the actor's synchronized block.
         * Default implementation does nothing.
         */
        protected void consume() {
        }
    }

    /**
     * A lock is turned on or off permanently 
     */
    public class Lockup extends Pin {
        
        public void on() {
            boolean doFire;
            lock.lock();
            try {
                doFire=turnOn();
            } finally {
                lock.unlock();
            }
            if (doFire) {
                fire();
            }
        }

        public void off() {
            lock.lock();
            try {
               turnOff();
            }
            finally {
              lock.unlock();
            }
        }
    }

    /**
     * holds tokens without data 
     */
    public class Semafor extends Pin {
        private int count=0;
        
        /** increments resource counter */
        public void up() {
            boolean doFire;
            lock.lock();
            try {
                count++;
                if (count!=1) {
                    return;
                }
                doFire=turnOn();
            } finally {
              lock.unlock();
            }
            if (doFire) {
                fire();
            }
        }

        /** increments resource counter by delta */
        public void up(int delta) {
            lock.lock();
            try {
                boolean wasOff=(count==0);
                count+=delta;
                if (wasOff && count>0) {
                    turnOn();
                }
            } finally {
              lock.unlock();
            }
        }

        /** decrements resource counter */
        public void down() {
            lock.lock();
            try {
                consume();
            }
            finally {
              lock.unlock();
            }
        }

        /** sets resource counter to 0 */
        public void clear() {
            lock.lock();
            try {
                count=0;
                turnOff();
            } finally {
              lock.unlock();
            }
        }

        @Override
        protected void consume() {
            if (count==0) {
                return;
            }
            count--;
            if (count==0) {
                turnOff();
            }
        }
    }

    /**
     * Token storage with standard Port<T> interface.
     * By default, it has place for only one token.
     * @param <T> type of accepted tokens.
     */
    public class Input<T> extends Pin implements StreamPort<T>, Iterable<T>{
        /** extracted token */
        T value=null;
        boolean pushback=false; // if true, do not consume
        private boolean closeRequested=false;

        @Override
        public void post(T token) {
            if (token==null) {
                throw new NullPointerException();
            }
            boolean doFire;
            lock.lock();
            try {
                if (closeRequested) {
                    throw new IllegalStateException("closed already");
                }
                if (value==null) {
                    value=token;
                    doFire=turnOn();
                } else {
                    add(token);
                    return; // is On already
                }
            } finally {
              lock.unlock();
            }
            if (doFire) {
                fire();
            }
        }

        /** Signals the end of the stream. 
         * Turns this pin on. Removed value is null 
         * (null cannot be send with StreamInput.add(message)).
         */
        @Override
        public void close() {
            boolean doFire;
            lock.lock();
            try {
                if (closeRequested) {
                    return;
                }
                closeRequested=true;
                //System.out.println("close()");
                doFire=turnOn();
            } finally {
              lock.unlock();
            }
            if (doFire) {
                fire();
            }
        }

        public boolean isClosed() {
            lock.lock();
            try {
                return closeRequested;
            } finally {
              lock.unlock();
            }
        }

        /**
         * saves passed token
         * @param newToken
         */
        protected void add(T newToken) {
            throw new IllegalStateException();
        }
                
        public T get() {
            return value;
        }

        /** look ahead */
        public T getNext() {
            return value=poll();
        }

        /**
         * iterates over and removes all input tokens.   
         */
        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>(){
                @Override
                public boolean hasNext() {
                    return value!=null;
                }

                @Override
                public T next() {
                    T res=value;
                    value=poll();
                    return res;
                }

                @Override
                public void remove() {
                }
            };
        }

        //===================== backend
        
        /**
         * removes token from the storage
         * @return removed token
         */
        protected T poll() {
            return null;
        }

        public void pushback() {
            pushback=true;
        }

        protected void pushback(T value) {
            pushback=true;
            this.value=value;
        }

        @Override
        protected void consume() {
            if (pushback) {
                pushback=false;
                // value remains the same, the pin remains turned on
                return; 
            }
            boolean wasNull=(value==null);
            value = poll();
            if (value!=null) {
                return; // continue processing
            }
            // no more tokens; check closing
            if (wasNull) {
                turnOff(); // closing processed already
            }
            if (!closeRequested) {
                turnOff(); // closing not requested
            }
            // else make one more round with message==null
        }
    }

    /** A place for single unremovable token of type <T>
     * @param <T> 
     */
    public class ConstInput<T> extends Input<T> {

        /** restores value
         */
        @Override
        protected T poll() {
            return get();
        }
    }

    /** Scalar Input which also redirects failures 
     */
    public class CallbackInput<T> extends Input<T> implements Callback<T> {
        @Override
        public void postFailure(Throwable exc) {
            DataflowVariable.this.postFailure(exc);
        }
    }
        
    /** A Queue of tokens of type <T>
     * @param <T> 
     */
    public class StreamInput<T> extends Input<T> {
        private Queue<T> queue;

        public StreamInput() {
            this.queue = new LinkedList<T>();
        }

        public StreamInput(Queue<T> queue) {
            this.queue = queue;
        }

        @Override
        protected void add(T token) {
            queue.add(token);
        }

        @Override
        public T poll() {
            return queue.poll();
        }
    }

    /**
     * This pin carries demand(s) of the result.
     * Demand is two-fold: it is a pin, so firing possible only if
     * someone demanded the execution, and it holds consumer's port where
     * the result should be sent. 
     * @param <R>  type of result
     */
    public class Demand<R> extends Pin implements Promise<R>, Callback<R> {
        private ListenableFuture<R> listeners=new ListenableFuture<R>();

        /** indicates a demand
         * @param sink Port to send the result
         * @return 
         */
        @Override
        public Promise<R> addListener(Callback<R> sink) {
            boolean doFire;
            lock.lock();
            try {
                listeners.addListener(sink);
                doFire=turnOn();
            } finally {
              lock.unlock();
            }
            if (doFire) {
                fire();
            }
            return this;
        }

        /** satisfy demand(s)
         */
        @Override
        public void post(R m) {
            listeners.post(m);
        }

        @Override
        public void postFailure(Throwable exc) {
            listeners.postFailure(exc);
        }
    }
}
