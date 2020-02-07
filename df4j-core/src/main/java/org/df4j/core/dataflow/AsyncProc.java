/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.dataflow;

import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link AsyncProc} is the most primitive component of {@link Dataflow} graph.
 *  It plays the same role as basic blocks in a flow chart
 *  (see <a href="https://en.wikipedia.org/wiki/Flowchart">https://en.wikipedia.org/wiki/Flowchart</a>).
 * {@link AsyncProc} contains single predefined port to accept flow of control by call to the method {@link AsyncProc#start()}.
 * This embedded port is implicitly switched off when this {@link AsyncProc} is fired,
 * so the method {@link AsyncProc#start()} must be called again explicitly if next firings are required.
 * Unlike basic blocks in traditional flow charts, different {@link AsyncProc}s in the same {@link Dataflow} graph can run in parallel.
 *
 * {@link AsyncProc} can contain additional input and output ports
 * to exchange messages and signals with ports of other {@link AsyncProc}s in consistent manner.
 * {@link AsyncProc} is submitted for execution to its executor when all ports become ready, including the embedded control port.
 */
public abstract class AsyncProc extends Node<AsyncProc> implements Activity {
    protected ActorState state = ActorState.Created;

    /** is not encountered as a parent's child */
    private boolean daemon;
    /**
     * blocked initially, until {@link #start} called.
     */
    private ArrayList<ControlPort> ports = new ArrayList<>();
    private int blockingPortCount = 0;
    private Executor executor;
    private Timer timer;
    protected ControlPort controlport = new ControlPort();

    protected AsyncProc(Dataflow dataflow) {
        if (dataflow == null) {
            throw new IllegalArgumentException();
        }
        this.parent = dataflow;
        dataflow.enter(this);
    }

    public AsyncProc() {
        this(new Dataflow());
    }

    public Dataflow getDataflow() {
        return parent;
    }

    public ActorState getState() {
        return state;
    }

    public Timer getTimer() {
        bblock.lock();
        try {
            if (timer == null) {
                timer = parent.getTimer();
            }
            return timer;
        } finally {
            bblock.unlock();
        }
    }

    public void setDaemon(boolean daemon) {
        bblock.lock();
        try {
            if (this.daemon) {
                return;
            }
            this.daemon = daemon;
            if (parent != null) {
                parent.leave(this);
            }
        } finally {
            bblock.unlock();
        }
    }

    public boolean isDaemon() {
        bblock.lock();
        try {
            return daemon;
        } finally {
            bblock.unlock();
        }
    }

    /**
     * passes a control token to this {@link AsyncProc}.
     * This token is consumed when this block is submitted to an executor.
     */
    public void start() {
        bblock.lock();
        try {
            if (state != ActorState.Created) {
                return;
            }
            if (_controlportUnblock()) {
                return;
            }
        } finally {
            bblock.unlock();
        }
        fire();
    }

    protected boolean _controlportUnblock() {
        if (controlport._unblock()) {
            state = ActorState.Waiting;
            return true;
        } else {
            state = ActorState.Running;
            return false;
        }
    }

    /**
     * finishes parent activity normally.
     */
    public void onComplete() {
        bblock.lock();
        try {
            if (isCompleted()) {
                return;
            }
            state = ActorState.Completed;
            super.onComplete();
            if (parent != null && !daemon) {
                parent.leave(this);
            }
        } finally {
            bblock.unlock();
        }
    }

    /**
     * finishes parent activity exceptionally.
     * @param ex the exception
     */
    protected void onError(Throwable ex) {
        bblock.lock();
        try {
            if (isCompleted()) {
                return;
            }
            state = ActorState.Completed;
            super.onError(ex);
        } finally {
            bblock.unlock();
        }
        parent.onError(ex);
    }

    public void setExecutor(Executor exec) {
        bblock.lock();
        try {
            this.executor = exec;
        } finally {
            bblock.unlock();
        }
    }

    public Executor getExecutor() {
        bblock.lock();
        try {
            if (executor == null) {
                executor = parent.getExecutor();
            }
            return executor;
        } finally {
            bblock.unlock();
        }
    }

    @Override
    public AsyncProc getItem() {
        return this;
    }

    /**
     * invoked when all asyncTask asyncTask are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    protected void fire() {
        getExecutor().execute(this::run);
    }

    @Override
    public boolean isAlive() {
        return !isCompleted();
    }

    /**
     * the main entry point.
     * Overwrite only to declare different kind of node.
     */
    protected void run() {
        try {
            runAction();
            onComplete();
        } catch (Throwable e) {
            onError(e);
        }
    }

    /**     * User's action.
     * User is adviswd top override this method, but overriding {@link #fire()} alse is possible
     *
     * @throws Throwable when thrown, this node is considered failed.
     */
    protected abstract void runAction() throws Throwable;

    private class ControlPort {
        protected boolean ready;

        public ControlPort(boolean ready) {
            this.ready = ready;
            bblock.lock();
            try {
                ports.add(this);
                if (!ready) {
                    blockingPortCount++;
                }
            } finally {
                bblock.unlock();
            }
        }

        public ControlPort() {
            this(false);
        }

        /**
         * must be invoked with locked {@link bblock}
         */
        protected synchronized void _block() {
            if (ready) {
                ready = false;
                blockingPortCount++;
            }
        }

        /**
         * must be invoked with locked {@link #bblock}
         *
         * @return true if become (or was) unblocked (ready)
         */
        protected boolean _unblock() {
            if (ready) {
                return true;
            }
            if (blockingPortCount == 0) {
                throw new IllegalStateException("blocked port and blockingPortCount == 0");
            } else if (blockingPortCount == 1) {
                return false; //      do      fire();
            }
            blockingPortCount--;
            ready = true;
            return true;
        }
    }

    /**
     * Basic class for all ports (places for tokens).
     * Has 2 states: ready or blocked.
     * When all ports become unblocked, method {@link AsyncProc#fire()} is called.
     * This resembles firing of a Petri Net transition.
     */
    public class Port extends ControlPort {
        /** locking order is: {@link #plock} 1st, {@link #bblock} 2nd */
        protected final Lock plock = new ReentrantLock();

        public Port(boolean ready) {
            super(ready);
        }

        protected AsyncProc getParent() {
            return AsyncProc.this;
        }

        public boolean isReady() {
            plock.lock();
            try {
                return ready;
            } finally {
                plock.unlock();
            }
        }

        private final void dbg(String s) {
//            System.out.println(BasicBlock.this.getClass().getName()+"/"+getClass().getSimpleName()+ s +blockingPortCount);
        }

        /**
         * sets this port to a blocked state.
         */
        public synchronized void block() {
            plock.lock();
            try {
                if (!ready) {
                    return;
                }
                ready = false;
                bblock.lock();
                try {
                    blockingPortCount++;
                } finally {
                    bblock.unlock();
                }
            } finally {
                plock.unlock();
            }
        }

        /**
         * sets this port to unblocked state.
         * If all ports become unblocked,
         * this block is submitted to the executor.
         */
        public void unblock() {
            plock.lock();
            try {
                if (ready) {
                    return;
                }
                ready = true;
                bblock.lock();
                try {
                    if (isCompleted()) {
                        return;
                    }
                    if (blockingPortCount == 0) {
                        throw new IllegalStateException("blocked port and blockingPortCount == 0");
                    }
                    blockingPortCount--;
       //             dbg("#unblock: blockingPortCount = "+blockingPortCount);
                    if (blockingPortCount > 0) {
                        return;
                    }
                    controlport._block();
                    state = ActorState.Running;
                } finally {
                    bblock.unlock();
                }
            } finally {
                plock.unlock();
            }
            fire();
        }

        @Override
        public String toString() {
            return super.toString() + (ready?": ready":": blocked");
        }

        protected Dataflow getDataflow() {
            return parent;
        }
    }
}