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

import javax.sound.sampled.Port;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link AsyncProc} is the base class of all active components of {@link Dataflow} graph.
 * Used in this basic form, it allows to construct asynchronous procedure calls.
 * {@link AsyncProc} contains single predefined port to accept flow of control by call to the method {@link AsyncProc#start()}.\
 * As a {@link Node}, it is descendand of {@link org.df4j.core.communicator.Completion} class, which allows to monitor execution
 * of this asynchonous procedure both with synchronous and asynchronous interfaces.
 * {@link AsyncProc} usually contains contain additional input and output ports to exchange messages and signals with
 * other {@link AsyncProc}s in consistent manner.
 * The lifecycle of any  {@link AsyncProc} is as follows:
 * {@link ActorState#Created} => {@link ActorState#Blocked} => {@link ActorState#Running} => {@link ActorState#Completed}.
 * It moves to {@link ActorState#Blocked} as a result of invocation of {@link AsyncProc#start()} method.
 * It becomes  {@link ActorState#Running} and is submitted for execution to its executor when all ports become ready.
 * It becomes {@link ActorState#Completed} when its method {@link AsyncProc#runAction()} completes, normally or exceptionally.
 */
public abstract class AsyncProc extends Node<AsyncProc> {
    private static final boolean checkingMode = true;

    protected ActorState state = ActorState.Created;

    /** is not encountered as a parent's child */
    private boolean daemon;
    /**
     * blocked initially, until {@link #start} called.
     */
    private ArrayList<Port> ports = new ArrayList<>(4);
    private int blockedPortsCount = 0;
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

    public ActorState getState() {
        return state;
    }

    public void setDaemon(boolean daemon) {
        synchronized(this) {
            if (this.daemon) {
                return;
            }
            this.daemon = daemon;
            leaveParent();
        }
    }

    public boolean isDaemon() {
        synchronized(this) {
            return daemon;
        }
    }

    /**
     * passes a control token to this {@link AsyncProc}.
     * This token is consumed when this block is submitted to an executor.
     */
    public void start() {
        synchronized(this) {
            if (state != ActorState.Created) {
                return;
            }
            state = ActorState.Blocked;
        }
        controlport.unblock();
    }

    /**
     * finishes parent activity normally.
     */
    public void onComplete() {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            state = ActorState.Completed;
        }
        super.onComplete();
    }

    /**
     * finishes parent activity exceptionally.
     * @param ex the exception
     */
    public void onError(Throwable ex) {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            state = ActorState.Completed;
        }
        super.onError(ex);
    }

    protected  void checkPorts() {
        if (checkingMode) {
            for (int k=0; k<ports.size(); k++) {
                Port port = ports.get(k);
                if (!port.isActive()) {
                    continue;
                }
                boolean mustBeBeBlocked = port == controlport;
                if (mustBeBeBlocked == port.ready) {
                    throw new IllegalStateException(" attempt to fire with wrong port state");
                }
            }
        }
    }

    protected  void checkBlockedPortsCount() {
        if (checkingMode) {
            int actualBlockedPortsCount = 0;
            for (int k=0; k<ports.size(); k++) {
                Port port = ports.get(k);
                if (port.isActive() && !port.isReady()) {
                    actualBlockedPortsCount++;
                }
            }
            if (actualBlockedPortsCount != blockedPortsCount) {
                throw new IllegalStateException("actual blockedPortsCount="+actualBlockedPortsCount+" but blockedPortsCount = "+blockedPortsCount);
            }
        }
    }

    /**
     * invoked when all asyncTask asyncTask are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    protected void fire() {
        checkPorts();
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
     * User is adviswd top override this method, but overriding {@link #fire()} is also possible
     *
     * @throws Throwable when thrown, this node is considered failed.
     */
    protected abstract void runAction() throws Throwable;

    @Override
    public String toString() {
        return super.toString() + "/"+state;
    }

    public String portsToString() {
        return ports.toString();
    }

    public interface PortI {
        /** make port not ready */
        void block();

        /** make port ready */
        void unblock();

        boolean isReady();
    }

    /**
     * Basic class for all ports (places for tokens).
     * Has 2 states: ready or blocked.
     * When all ports become unblocked, method {@link AsyncProc#fire()} is called.
     * This is clear analogue to the firing of a Petri Net transition.
     */
    public static class Port implements PortI {
        /** locking order is: {@link #plock} 1st, {@link #bblock} 2nd */
        protected final Lock plock = new ReentrantLock();
        protected boolean active;
        protected boolean ready = false;
        private AsyncProc parent;

        public Port(AsyncProc parent, boolean ready, boolean active) {
            this.parent = parent;
            boolean blocked = !ready && active;
            synchronized(parent) {
                parent.ports.add(this);
                if (blocked) {
                    parent.blockedPortsCount++;
                }
            }
            this.ready = ready;
            this.active = active;
        }

        public Port(AsyncProc parent, boolean ready) {
            this(parent, ready, true);
        }

        public Port(AsyncProc parent) {
            this(parent, false);
        }

        protected AsyncProc getParent() {
            return parent;
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

        public void setActive(boolean active) {
            plock.lock();
            try {
                parent.checkBlockedPortsCount();
                boolean wasActive = this.active;
                if (wasActive == active) {
                    return;
                }
                this.active = active;
                boolean wasBlocked = !ready && wasActive;
                boolean needBlocked = !ready && active;
                if (wasBlocked == needBlocked) {
                    return;
                }
                synchronized(parent) {
                    if (parent.isCompleted()) {
                        return;
                    }
                    if (needBlocked) {
                        parent.blockedPortsCount++;
                        parent.checkBlockedPortsCount();
                        return;
                    }
                    boolean doreturn = _decBlockedPortsCount();
                    parent.checkBlockedPortsCount();
                    if (doreturn) {
                        return;
                    }
                }
            } finally {
                plock.unlock();
            }
            parent.fire();
        }

        /**
         * sets this port to a blocked state.
         */
        public void block() {
            plock.lock();
            try {
                parent.checkBlockedPortsCount();
                if (!ready) {
                    return;
                }
                ready = false;
                if (!active) {
                    return;
                }
                synchronized(parent) {
                    if (parent.isCompleted()) {
                        return;
                    }
                    parent.blockedPortsCount++;
                    parent.checkBlockedPortsCount();
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
                synchronized(parent) {
                    if (parent.isCompleted()) {
                        return; // do not fire
                    }
                    boolean doReturn = _decBlockedPortsCount();
                    if (doReturn) {
                        return;
                    }
                }
            } finally {
                plock.unlock();
            }
            parent.fire();
        }

        /**
         * call only when {@link #bblock} locked
         * @return true if still in blocked state
         *         false when must fire
         */
        private boolean _decBlockedPortsCount() {
            if (parent.blockedPortsCount == 0) {
                throw new IllegalStateException("port blocked but blockingPortCount == 0");
            }
            parent.blockedPortsCount--;
            if (parent.blockedPortsCount > 0) {
                return true; // do not fire
            }
            parent.controlport.block();
            parent.state = ActorState.Running;
            return false; // do fire
        }

        @Override
        public String toString() {
            return super.toString() + (ready?": ready":": blocked");
        }
    }

    public class ControlPort extends Port {
        public ControlPort() {
            super(AsyncProc.this);
        }
    }
}