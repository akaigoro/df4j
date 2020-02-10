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
    protected ControlPort controlport = new ControlPort();

    protected AsyncProc(Dataflow dataflow) {
        super(dataflow);
        if (dataflow == null) {
            throw new IllegalArgumentException();
        }
    }

    public AsyncProc() {
        this(new Dataflow());
    }

    @Override
    public AsyncProc getItem() {
        return this;
    }

    public ActorState getState() {
        return state;
    }

    public void setDaemon(boolean daemon) {
        bblock.lock();
        try {
            if (this.daemon) {
                return;
            }
            this.daemon = daemon;
            leaveParent();
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
        } finally {
            bblock.unlock();
        }
        super.onComplete();
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
        } finally {
            bblock.unlock();
        }
        super.onError(ex);
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

    @Override
    public String toString() {
        return super.toString() + "/"+state;
    }

    private class ControlPort {
        protected boolean ready = false;

        public ControlPort(boolean blocking) {
            bblock.lock();
            try {
                ports.add(this);
                if (blocking) {
                    blockingPortCount++;
                }
            } finally {
                bblock.unlock();
            }
        }

        public ControlPort() {
            this(true);
        }

        /**
         * must be invoked with locked {@link #bblock}
         */
        private synchronized void _block() {
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
        private boolean _unblock() {
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

        @Override
        public String toString() {
            return super.toString() + (ready?": ready":": blocked");
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
        protected boolean active;

        public Port(boolean ready, boolean active) {
            super(active && !ready);
            this.ready = ready;
            this.active = active;
        }

        public Port(boolean ready) {
            this(ready, true);
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

        public boolean isActive() {
            plock.lock();
            try {
                return active;
            } finally {
                plock.unlock();
            }
        }

        private boolean decrBlockedPortCount() {
            bblock.lock();
            try {
                if (isCompleted()) {
                    return true;
                }
                if (blockingPortCount == 0) {
                    throw new IllegalStateException("port blocked but blockingPortCount == 0");
                }
                blockingPortCount--;
                if (blockingPortCount > 0) {
                    return true;
                }
                controlport._block();
                state = ActorState.Running;
            } finally {
                bblock.unlock();
            }
            return false;
        }

        public void setActive(boolean active) {
            plock.lock();
            try {
                boolean wasActive = this.active;
                if (wasActive == active) {
                    return;
                }
                this.active = active;
                if (ready) {
                    return;
                }
                bblock.lock();
                try {
                    if (isCompleted()) {
                        return;
                    }
                    if (active) {
                        blockingPortCount++;
                        return;
                    }
                    if (decrBlockedPortCount()) {
                        return;
                    }
                } finally {
                    bblock.unlock();
                }
            } finally {
                plock.unlock();
            }
            fire();;
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
                if (!active) {
                    return;
                }
                bblock.lock();
                try {
                    if (isCompleted()) {
                        return;
                    }
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
                if (!active) {
                    return;
                }
                if (decrBlockedPortCount()) {
                    return;
                }
            } finally {
                plock.unlock();
            }
            fire();
        }
    }
}