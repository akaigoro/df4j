package com.github.rfqu.df4j.ringbuffer;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.DataflowNode;

/**
 * The intermediate passing node
 * 
 */
public abstract class BoltNode extends DataflowNode {
    protected Callback<Object> context;
    protected Lockup lockUp = new Lockup();

    public void setContext(Callback<Object> context) {
        this.context=context;
    }

    public void start() {
        lockUp.on();
    }

    public void stop() {
        lockUp.off();
    }

    /**
     * Ring buffer access 
     * Serves both as an imput and output
     */
    public class Window<T> extends Semafor {
        RingBuffer<T>.Window rbw;

        public void setRbw(RingBuffer<T>.Window rbw) {
            this.rbw = rbw;
        }

        public T get() {
            return rbw.get();
        }

        public void post(T element) {
            rbw.post(element);
        }

        public boolean isClosed() {
            // TODO Auto-generated method stub
            return false;
        }
    }
}