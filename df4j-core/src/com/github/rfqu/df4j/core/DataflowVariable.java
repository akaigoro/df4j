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
                        if (pin.consume()==0) {
                        	pin.turnOff();
                        }
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

        @Override
        public String toString() {
            return DataflowVariable.this.toString();
        }
    }

    /**
     * Basic place for input tokens.
     * Initial state should be empty, to prevent premature firing.
     */
    protected abstract class Pin {
        private final Pin next; // link to list
        private final int pinBit; // distinct for all other pins of the node 

        protected Pin() {
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
        final boolean turnOn() {
            //System.out.print("turnOn "+fired+" "+allReady());
            pinOn(pinBit);
            if (allReady()) {
                fireLock(); // to prevent multiple concurrent firings
                //System.out.println(" => true");
                return true;
            } else {
                //System.out.println(" => false");
                return false;
            }
        }

        /**
         * sets pin's bit off
         */
        protected final void turnOff() {
            //System.out.println("turnOff");
            pinOff(pinBit);
        }

        /** Executed after token processing (method act).
         * Cleans reference to value, if any.
         * Signals to set state to off if no more tokens are in the place.
         * Should return quickly, as is called from the actor's synchronized block.
         * Default implementation does nothing.
         * @return -1 when end of stream reached
         *          0 if no data available
         *          1 if next token provided 
         */
        protected abstract int consume();
    }


    //============== stuff for extending Pin from another package without showing up lock
    public abstract class PinBase<T> extends Pin {
        
        /** 
         * @return true if Pin turned on
         */
        protected abstract boolean turnedOn(T token);
        
        protected final void checkOn(T token) {
            boolean doFire;
            lock.lock();
            try {
                if (turnedOn(token)) {
                    doFire=turnOn();
                } else {
                    return;
                }
            } finally {
                lock.unlock();
            }
            if (doFire) {
                fire();
            }
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

		@Override
		protected int consume() {
			return 1;// do not turn off
		}
    }

    /**
     * holds token counter without data
     * counter can be negative 
     */
    public class Semafor extends Pin {
        private int count;
        
        public Semafor() {
            this.count = 0;
        }

        public Semafor(int count) {
            if (count>0) {
                throw new IllegalArgumentException("initial counter cannot be positive");
            }
            this.count = count;
        }

        /** increments resource counter by 1 */
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
            boolean doFire;
            lock.lock();
            try {
                boolean wasOff=(count<=0);
                count+=delta;
                boolean isOff=(count<=0);
                if (wasOff == isOff) {
                    return;
                }
                if (isOff) {
                    turnOff();
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

        /** decrements resource counter */
        public void down() {
            lock.lock();
            try {
                if (consume()==0) {
                    turnOff();
                }
            }
            finally {
              lock.unlock();
            }
        }

        @Override
        protected int consume() {
            if (--count==0) {
                return 0;
            } else {
            	return 1;
            }
        }
    }

    /**
     * Token storage with standard Port<T> interface.
     * By default, it has place for only one token.
     * @param <T> type of accepted tokens.
     */
    public class Input<T> extends Pin implements Port<T> {
        /** extracted token */
        T value=null;
        boolean pushback=false; // if true, do not consume

        @Override
        public void post(T token) {
            if (token==null) {
                throw new NullPointerException();
            }
            boolean doFire;
            lock.lock();
            try {
                if (value==null) {
                    value=token;
                    doFire=turnOn();
                } else {
                    throw new IllegalStateException();
                }
            } finally {
              lock.unlock();
            }
            if (doFire) {
                fire();
            }
        }

		public T get() {
			return value;
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
        protected int consume() {
            if (pushback) {
                pushback=false;
                // value remains the same, the pin remains turned on
                return 1; 
            }
            boolean wasNull=(value==null);
            value = poll();
            if (value!=null) {
                return 1; // continue processing
            }
            // no more tokens; check closing
            if (wasNull) {
            	return 0; // closing processed already
            }
            return -1; // else make one more round with message==null
        }
    }

    /** A Queue of tokens of type <T>
     * @param <T> 
     */
    public class StreamInput<T> extends Input<T> implements StreamPort<T> {
        private Queue<T> queue;
        private boolean closeRequested=false;

        public StreamInput() {
            this.queue = new LinkedList<T>();
        }

        public StreamInput(Queue<T> queue) {
            this.queue = queue;
        }
        
        public T get() {
            return value;
        }

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

        protected void add(T token) {
            queue.add(token);
        }

        @Override
        public T poll() {
            return queue.poll();
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


        /** dryRun consume()
         * 
         * @return
         */
        public int hasNext() {
            lock.lock();
            try {
                if (pushback || !queue.isEmpty()) {
                    return 1; // continue processing
                }
                // no more tokens; check closing
                if ((value!=null) && closeRequested) {
                    return -1; // EOF, process end of stream with message==null
                } else {
                	return 0;
                }
            } finally {
              lock.unlock();
            }
        }

        @Override
        protected int  consume() {
            if (pushback) {
                pushback=false;
                // value remains the same, the pin remains turned on
                return 1; 
            }
            boolean wasNull=(value==null);
            value = poll();
            if (value!=null) {
                return 1; // continue processing
            }
            // no more tokens; check closing
            if (!wasNull && closeRequested) {
                return -1; // EOF, process end of stream with message==null
            } else {
            	return 0;
            }
        }

        /**
         */
        public T moveNext() {
            lock.lock();
            try {
                consume();
                return value;
            } finally {
              lock.unlock();
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
    }
}
