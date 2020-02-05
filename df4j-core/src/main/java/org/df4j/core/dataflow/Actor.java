package org.df4j.core.dataflow;

import function.ThrowingRunnable;

/**
 * {@link Actor} is a {@link Dataflow} with single {@link AsyncProc} which is executed in a loop.
 * In other words, Actor is a repeatable asynchronous procedure.
 *  `Actors` here are <a href="https://pdfs.semanticscholar.org/2dfa/fb6ea86ac739b17641d4c4e51cc17d31a56f.pdf"><i>dataflow actors whith arbitrary number of parameters.</i></a>
 *  An Actor as designed by Carl Hewitt is just an {@link Actor} with single input port.
 */
public abstract class Actor extends AsyncProc {
    private ThrowingRunnable tRunnable;
    private long delay = 0l;

    public Actor(Dataflow parent) {
        super(parent);
    }

    public Actor() {
    }

    protected void nextAction(ThrowingRunnable tRunnable) {
        this.tRunnable = tRunnable;
    }

    protected void delay(long delay) {
        if (delay <= 0) {
            delay = 0;
        }
        this.delay = delay;
    }


    /**
     * sets infinite delay
     *
     */
    protected void stop() {
        this.delay = -1;
    }

    @Override
    public void start() {
        nextAction(this::runAction);
        super.start();
    }

    @Override
    protected void run() {
        try {
            tRunnable.run();
            if (this.isCompleted()) {
                return;
            }
            if (delay == 0l) {
                this.start(); // make loop
            } else if (delay > 0l) {
                long d = this.delay;
                this.delay = 0l;
                start(d);
            } // else do not awake
        } catch (Throwable e) {
            onError(e);
        }
    }

}
