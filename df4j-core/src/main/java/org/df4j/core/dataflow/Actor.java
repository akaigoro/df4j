package org.df4j.core.dataflow;

import java.util.TimerTask;

import static org.df4j.core.dataflow.ActorState.*;

/**
 * {@link Actor} is an {@link AsyncProc} whose {@link Actor#runAction()} method can be executed repeatedly,
 * if its input ports receives more input arguments.
 * In other words, Actor is a repeatable asynchronous procedure.
 *  `Actors` here are <a href="https://pdfs.semanticscholar.org/2dfa/fb6ea86ac739b17641d4c4e51cc17d31a56f.pdf"><i>dataflow actors whith arbitrary number of parameters.</i></a>
 *  An Actor as designed by Carl Hewitt is just an {@link Actor} with single input port, and is implemented as {@link SimpleActor}.
 */
public abstract class Actor extends AsyncProc {
    private volatile ThrowingRunnable nextAction;
    private TimerTask task;

    {nextAction(this::runAction);}

    public Actor(Dataflow parent) {
        super(parent);
    }

    public Actor() {
    }

    public ThrowingRunnable getNextAction() {
        return nextAction;
    }

    protected void nextAction(ThrowingRunnable tRunnable) {
        this.nextAction = tRunnable;
    }

    /**
     * setes delay before subsequent call to next action.
     * Previousely set delay is canceled.
     *
     * @param delay delay in milliseconds. Value &le; 0 means no delay.
     */
    protected void delay(long delay) {
        synchronized(this) {
            if (state == Completed) {
                return;
            }
            if (delay <= 0) {
                return;
            }
            state = Suspended;
            this.task = new MyTimerTask();
        }
        getTimer().schedule(task, delay);
    }

    /**
     * sets infinite delay. Previously set delay is canceled.
     */
    protected synchronized void suspend() {
        if (state == Completed) {
            return;
        }
        state = Suspended;
    }

    /**
     * Moves this actor from {@link ActorState#Suspended} state to {@link ActorState#Blocked} or {@link ActorState#Running}.
     * Ignored if current state is not {@link ActorState#Suspended}.
     */
    public  void resume() {
        synchronized(this) {
            if (state != Suspended) {
                return;
            }
            if (this.task != null) {
                this.task.cancel();
                this.task = null;
            }
            state = Blocked;
        }
        controlport.unblock();
    }

    @Override
    protected void run() {
        try {
            nextAction.run();
            synchronized (this) {
                switch (state) {
                    case Completed:
                    case Suspended:
                    case Blocked:
                    return;
                }
                state = Blocked;
            }
            controlport.unblock();
        } catch (Throwable e) {
            super.onError(e);
        }
    }

    private class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            resume();
        }
    }
}
