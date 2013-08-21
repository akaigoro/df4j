package com.github.rfqu.df4j.core;

import java.util.Deque;
import java.util.LinkedList;
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
    private final Lock lock = new ReentrantLock();
    private Throwable exc=null;
    private Pin head; // the head of the list of Pins
    private int pinCount=1; // fire bit allocated
    private int readyPins=0;  // mask with 0 for ready pins, 1 for blocked
    private final Task task; 
    
    public DataflowVariable(RunnableTask task) {
        this.task=task;
        if (task instanceof NodeTask) {
            ((NodeTask)task).outer=this;
        }
    }

    public DataflowVariable() {
        task=new SyncTask();
    }


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
            task.fire();
        }
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
            for (;;) {
                act();
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

    private final void loopActNoConsume() {
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
            lock.lock();
            try {
                if (allInputsReady()) {
                    exc=new RuntimeException(this.getClass().getName()+"#act returned in ready state");
                } else {
                    fireUnlock(); // allow firing
                    return;
                }
            }
            finally {
              lock.unlock();
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
    protected class SyncTask implements Task {
        
        @Override
        public void fire() {
            loopAct();
        }
    }
    
    private static abstract class NodeTask extends RunnableTask {
        DataflowVariable outer;
        
        protected NodeTask(Executor executor) {
            super(executor);
        }

    }

    protected static class ActorTask extends NodeTask {
        
        public ActorTask(Executor executor) {
            super(executor);
        }

        /** loops while all pins are ready
         */
        @Override
        public void run() {
            //System.out.println("ActorTask run");
            outer.loopAct();
        }
    }

    protected static class VertexTask extends NodeTask {
        
        public VertexTask(Executor executor) {
            super(executor);
        }

        /** loops while all pins are ready
         */
        @Override
        public void run() {
            //System.out.println("ActorTask run");
            outer.loopActNoConsume();
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
        protected void consume(){}
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
                task.fire();
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
                task.fire();
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
		protected void consume() {
			return;// do not turn off
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
                task.fire();
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
                task.fire();
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

        @Override
        protected void consume() {
            if (--count==0) {
                turnOff();
            }
        }
    }

    /**
     * Token storage with standard Port<T> interface.
     * It has place for only one token, which is not consumed.
     * @param <T> type of accepted tokens.
     */
    public class ConstInput<T> extends Pin implements Port<T> {
        /** extracted token */
        T value=null;

        @Override
        public void post(T token) {
            if (token==null) {
                throw new NullPointerException();
            }
            boolean doFire;
            lock.lock();
            try {
                if (value!=null) {
                    throw new IllegalStateException("token set already");
                }
                value=token;
                doFire=turnOn();
            } finally {
              lock.unlock();
            }
            if (doFire) {
                task.fire();
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
    }


    /**
     * Token storage with standard Port<T> interface.
     * By default, it has place for only one token.
     * @param <T> type of accepted tokens.
     */
    public class Input<T> extends ConstInput<T> implements Port<T> {
        boolean pushback=false; // if true, do not consume

        public T get() {
            return value;
        }

        //===================== backend

        public void pushback() {
            if (pushback) {
                throw new IllegalStateException();
            }
            pushback=true;
        }

        protected void pushback(T value) {
            if (pushback) {
                throw new IllegalStateException();
            }
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
            // check closing
            if ((value==null)) {
                turnOff(); // closing processed already
            }
            // else make one more round with value==null
            value = null;
        }
    }

    /** A Queue of tokens of type <T>
     * @param <T> 
     */
    public class StreamInput<T> extends Input<T> implements StreamPort<T> {
        private Deque<T> queue;
        private boolean closeRequested=false;

        public StreamInput() {
            this.queue = new LinkedList<T>();
        }

        public StreamInput(Deque<T> queue) {
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
                task.fire();
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
                task.fire();
            }
        }


        @Override
        public void pushback() {
            if (pushback) {
                throw new IllegalStateException();
            }
            pushback=true;
        }

        @Override
        protected void pushback(T value) {
            if (!pushback) {
                pushback=true;
            } else {
                queue.addFirst(this.value);
                this.value=value;
            }
        }

        /** 
         * attempt to take next token from the input queue
         * @return true if next token is available, or if stream is closed
         *   false if input queue is empty
         */
        public boolean moveNext() {
            lock.lock();
            try {
                if (pushback) {
                    pushback=false;
                    return true;
                }
                T newValue=queue.poll();
				if (newValue!=null) {
					value=newValue;
                    return true;
				} else if (closeRequested) {
					value=null;
	                return true;
				} else {
					return false;
				}
            } finally {
                lock.unlock();
            }
        }
        
        @Override
        protected void  consume() {
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

        public boolean isClosed() {
            lock.lock();
            try {
                return closeRequested && (value==null);
            } finally {
              lock.unlock();
            }
        }
    }
}
