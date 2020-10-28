package org.df4j.core.actor;

import java.util.TimerTask;

/**
 * {@link Actor} is an {@link AsyncProc} whose {@link Actor#runAction()} method can be executed repeatedly,
 * if its input ports receives more input arguments.
 * In other words, Actor is a repeatable asynchronous procedure.
 *  `Actors` here are <a href="https://pdfs.semanticscholar.org/2dfa/fb6ea86ac739b17641d4c4e51cc17d31a56f.pdf"><i>dataflow actors whith arbitrary number of parameters.</i></a>
 *  An Actor as designed by Carl Hewitt is just an {@link Actor} with single input port, and is implemented as {@link ClassicActor}.
 */
public abstract class Actor extends AsyncProc {
    public static final int PORTS_ALL  = 0xFFFFFFFF;
    public static final int PORTS_NONE = 0x00000001;
    private int activePortsScale = PORTS_ALL;
    private volatile ThrowingRunnable nextAction;
    private TimerTask task;

    {nextAction(this::runAction);}

    public Actor(ActorGroup parent) {
        super(parent);
    }

    public Actor() {
    }

    @Override
    protected TransitionAll createTransition() {
        return new TransitionSome();
    }

    public static int makePortScale(Port... ports) {
        int scale = 0;
        for (Port port: ports) {
            scale |= (1 << port.portNum);
        }
        return scale;
    }

    protected void setActivePorts(int scale) {
        activePortsScale = PORTS_NONE | scale;
    }

    protected void setActivePorts(Port... ports) {
        setActivePorts(makePortScale(ports));
    }

    public ThrowingRunnable getNextAction() {
        return nextAction;
    }

    protected void nextAction(ThrowingRunnable tRunnable, int portScale) {
        this.nextAction = tRunnable;
        setActivePorts(portScale);
    }

    protected void nextAction(ThrowingRunnable tRunnable, Port... ports) {
        nextAction(tRunnable, makePortScale(ports));
    }

    protected void nextAction(ThrowingRunnable tRunnable) {
        nextAction(tRunnable, PORTS_ALL);
    }

    /**
     * setes delay before subsequent call to next action.
     * Previousely set delay is canceled.
     *
     * @param delay delay in milliseconds. Value &le; 0 means no delay.
     */
    protected void delay(long delay) {
        synchronized(this) {
            if (state == ActorState.Completed) {
                return;
            }
            if (delay <= 0) {
                return;
            }
            state = ActorState.Suspended;
            this.task = new MyTimerTask();
        }
        getTimer().schedule(task, delay);
    }

    /**
     * sets infinite delay. Previously set delay is canceled.
     */
    protected synchronized void suspend() {
        if (state == ActorState.Completed) {
            return;
        }
        state = ActorState.Suspended;
    }

    /**
     * Moves this actor from {@link ActorState#Suspended} state to {@link ActorState#Blocked} or {@link ActorState#Running}.
     * Ignored if current state is not {@link ActorState#Suspended}.
     */
    public  void resume() {
        synchronized(this) {
            if (state != ActorState.Suspended) {
                return;
            }
            if (this.task != null) {
                this.task.cancel();
                this.task = null;
            }
            _controlportUnblock();
        }
    }

    @Override
    protected void run() {
        try {
            nextAction.run();
            synchronized (this) {
                switch (state) {
                    case Completed:
                    case Suspended:
                    return;
                default:
                    _controlportUnblock();
                }
            }
        } catch (Throwable e) {
            super.complete(e);
        }
    }

    private class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            resume();
        }
    }

    class TransitionSome extends TransitionAll {

        @Override
        public boolean canFire() {
            return (blockedPortsScale & activePortsScale) == 0;
        }
    }

    @FunctionalInterface
    public static interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
