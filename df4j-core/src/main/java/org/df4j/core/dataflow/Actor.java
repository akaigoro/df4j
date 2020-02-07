package org.df4j.core.dataflow;

import function.ThrowingRunnable;

import java.util.TimerTask;

import org.df4j.core.dataflow.ActorState;
import static org.df4j.core.dataflow.ActorState.*;

/**
 * {@link Actor} is a {@link Dataflow} with single {@link AsyncProc} which is executed in a loop.
 * In other words, Actor is a repeatable asynchronous procedure.
 *  `Actors` here are <a href="https://pdfs.semanticscholar.org/2dfa/fb6ea86ac739b17641d4c4e51cc17d31a56f.pdf"><i>dataflow actors whith arbitrary number of parameters.</i></a>
 *  An Actor as designed by Carl Hewitt is just an {@link Actor} with single input port.
 */
public abstract class Actor extends AsyncProc {
    private volatile ThrowingRunnable nextAction;
    private long delay = 0l;
    private TimerTask task;
    {
        setNextAction(this::runAction);}

    public Actor(Dataflow parent) {
        super(parent);
    }

    public Actor() {
    }

    public ThrowingRunnable getNextAction() {
        return nextAction;
    }

    protected void setNextAction(ThrowingRunnable tRunnable) {
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
        bblock.lock();
        try {
            if (state != Created) {
                return;
            }
            state = Running;
        } finally {
            bblock.unlock();
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
        bblock.lock();
        try {
            if (state == Completed) {
                return;
            }
            if (delay <= 0) {
                delay = 0;
            }
            this.delay = delay;
        } finally {
            bblock.unlock();
        }
    }

    /**
     * sets infinite delay. Previously set delay is canceled.
     */
    protected void suspend() {
        bblock.lock();
        try {
            if (state == Completed) {
                return;
            }
            this.delay = -1;
        } finally {
            bblock.unlock();
        }
    }

    /**
     * Moves this actor from {@link ActorState#Suspended} state to {@link ActorState#Waiting) or {@link ActorState#Running}.
     * Ignored if current state is not  {@link ActorState#Suspended}.
     */
    public void resume() {
        bblock.lock();
        try {
            if (state != Suspended) {
                return;
            }
            this.delay = 0;
            if (this.task != null) {
                this.task.cancel();
                this.task = null;
            }
            if (_controlportUnblock()) {
                return;
            }
        } finally {
            bblock.unlock();
        }
        fire();
    }

    private void _restart() {
        bblock.lock();
        try {
            if (state == Completed) {
                return;
            }
            if (delay == 0l) {
                // make loop using fire()
                if (_controlportUnblock()) {
                    return;
                }
            } else {
                if (delay > 0l) { // normal delay
                    long d = this.delay;
                    this.delay = 0l;
                    this.task = new MyTimerTask();
                    getTimer().schedule(task, d);
                } // else infinite delay
                state = Suspended;
                return;
            }
        } finally {
            bblock.unlock();
        }
        fire();
    }

    @Override
    protected void run() {
        try {
            nextAction.run();
            _restart();
        } catch (Throwable e) {
            onError(e);
        }
    }

    private class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            resume();
        }
    }
}
