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
    private long delay = 0l;
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
     * moves this {@link Actor} from {@link ActorState#Created} state to {@link ActorState#Running}
     * (or {@link ActorState#Suspended}, if was suspended in constructor).
     *
     * Only the first call works, subsequent calls are ignored.
     */
    @Override
    public void start() {
        synchronized(this) {
            if (state != Created) {
                return;
            }
            state = ActorState.Blocked;
        }
        _restart();
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
                delay = 0;
            }
            this.delay = delay;
        }
    }

    /**
     * sets infinite delay. Previously set delay is canceled.
     */
    protected void suspend() {
        synchronized(this) {
            if (state == Completed) {
                return;
            }
            this.delay = -1;
        }
    }

    /**
     * Moves this actor from {@link ActorState#Suspended} state to {@link ActorState#Blocked} or {@link ActorState#Running}.
     * Ignored if current state is not {@link ActorState#Suspended}.
     */
    public void resume() {
        synchronized(this) {
            if (state != Suspended) {
                return;
            }
            this.delay = 0;
            if (this.task != null) {
                this.task.cancel();
                this.task = null;
            }
            controlport.unblock();
        }
    }

    private void _restart() {
        synchronized(this) {
            if (state == Completed) {
                return;
            }
            if (delay != 0l) {
                // make loop using fire()
                if (delay > 0l) { // normal delay
                    long d = this.delay;
                    this.delay = 0l;
                    this.task = new MyTimerTask();
                    getTimer().schedule(task, d);
                } // else infinite delay
                state = Suspended;
                return;
            }
            state = Blocked;
            controlport.unblock();
        }
    }

    @Override
    protected void run() {
        try {
            nextAction.run();
            _restart();
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
