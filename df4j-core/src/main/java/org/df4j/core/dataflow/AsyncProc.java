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
import static org.df4j.core.dataflow.ActorState.*;

/**
 * {@link AsyncProc} is the base class of all active components of {@link Dataflow} graph.
 * Used in this basic form, it allows to construct asynchronous procedure calls.
 * {@link AsyncProc} contains single predefined port to accept flow of control by call to the method {@link AsyncProc#start()}.\
 * As a {@link Node}, it is descendand of {@link org.df4j.core.communicator.Completion} class, which allows to monitor execution
 * of this asynchonous procedure both with synchronous and asynchronous interfaces.
 * {@link AsyncProc} usually contains contain additional input and output ports to exchange messages and signals with
 * other {@link AsyncProc}s in consistent manner.
 * The lifecycle of any  {@link AsyncProc} is as follows:
 * {@link ActorState#Created} &ge; {@link ActorState#Blocked} &ge; {@link ActorState#Running} &ge; {@link ActorState#Completed}.
 * It moves to {@link ActorState#Blocked} as a result of invocation of {@link AsyncProc#start()} method.
 * It becomes  {@link ActorState#Running} and is submitted for execution to its executor when all ports become ready.
 * It becomes {@link ActorState#Completed} when its method {@link AsyncProc#runAction()} completes, normally or exceptionally.
 */
public abstract class AsyncProc extends Node<AsyncProc> {
    public static final int MAX_PORT_NUM = 31;
    private static final boolean checkingMode = true; // todo false

    protected ActorState state = Created;

    /** is not encountered as a parent's child */
    private boolean daemon;
    /**
     * blocked initially, until {@link #start} called.
     */
    private ArrayList<Port> ports = new ArrayList<>(4);
    private int blockedPortsScale = 0;
    private ControlPort controlport = new ControlPort(this);

    protected AsyncProc(Dataflow dataflow) {
        super(dataflow);
    }

    public AsyncProc() {
        this(new Dataflow());
    }

    public ActorState getState() {
        return state;
    }

    public synchronized void setDaemon(boolean daemon) {
        if (this.daemon) {
            return;
        }
        this.daemon = daemon;
        leaveParent();
    }

    private void setBlocked(int portNum) {
        blockedPortsScale |= (1<<portNum);
    }

    protected int setUnBlocked(int portNum) {
        return blockedPortsScale &= ~(1<<portNum);
    }

    protected boolean isBlocked(int portNum) {
        return (blockedPortsScale & (1<<portNum)) != 0;
    }

    public synchronized boolean isDaemon() {
        return daemon;
    }

    /**
     * moves this {@link AsyncProc} from {@link ActorState#Created} state to {@link ActorState#Running}
     * (or {@link ActorState#Suspended}, if was suspended in constructor).
     *
     * In other words, passes the control token to this {@link AsyncProc}.
     * This token is consumed when this block is submitted to an executor.
     * Only the first call works, subsequent calls are ignored.
     */
    public synchronized void start() {
        if (state != Created) {
            return;
        }
        _controlportUnblock();
    }

    /**
     * finishes parent activity normally.
     */
    public void onComplete() {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            state = Completed;
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
            state = Completed;
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

    protected void _controlportUnblock() {
        state = Blocked;
        controlport.unblock();
    }

    protected void _controlportBlock() {
        state = Running;
        controlport.block();
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

    /**
     * Basic class for all ports (places for tokens).
     * Has 2 states: ready or blocked.
     * When all ports become unblocked, method {@link AsyncProc#fire()} is called.
     * This is clear analogue to the firing of a Petri Net transition.
     */
    public static class Port {
        protected boolean ready = false;
        protected AsyncProc parent;
        protected final int portNum;

        public Port(AsyncProc parent, boolean ready) {
            this.parent = parent;
            synchronized(parent) {
                portNum=parent.ports.size();
                if (portNum > MAX_PORT_NUM) {
                    throw new IllegalStateException("too many ports");
                }
                parent.ports.add(this);
                if (!ready) {
                    parent.setBlocked(portNum);
                }
            }
            this.ready = ready;
        }

        public Port(AsyncProc parent) {
            this(parent, false);
        }

        protected AsyncProc getParent() {
            return parent;
        }

        public boolean isReady() {
            synchronized(parent) {
                return ready;
            }
        }

        /**
         * sets this port to a blocked state.
         */
        public void block() {
            synchronized(parent) {
                if (!ready) {
                    return;
                }
                ready = false;
                if (parent.isCompleted()) {
                    return;
                }
                parent.setBlocked(portNum);
            }
        }

        /**
         * sets this port to unblocked state.
         * If all ports become unblocked,
         * this block is submitted to the executor.
         */
        public synchronized void unblock() {
            synchronized(parent) {
                if (ready) {
                    return;
                }
                ready = true;
                if (parent.isCompleted()) {
                    return;
                }
                if (parent.blockedPortsScale == 0) {
                    throw new IllegalStateException("port blocked but blockedPortsScale == 0");
                }
                if (parent.setUnBlocked(portNum) == 0) {
                    parent._controlportBlock();
                    parent.fire();
                }
            }
        }

        @Override
        public String toString() {
            return super.toString() + (ready?": ready":": blocked");
        }
    }

    private static class ControlPort extends Port {
        ControlPort(AsyncProc parent) {
            super(parent);
        }
    }
}