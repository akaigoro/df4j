package org.df4j.core.dataflow;

/**
 * {@link Actor} is a {@link Dataflow} with single {@link BasicBlock} which is executed in a loop.
 * In other words, Actor is a repeatable asynchronous procedure.
 *  `Actors` here are <a href="https://pdfs.semanticscholar.org/2dfa/fb6ea86ac739b17641d4c4e51cc17d31a56f.pdf"><i>dataflow actors whith arbitrary number of parameters.</i></a>
 *  An Actor as designed by Carl Hewitt can be modelled as {@link Actor} with single port of type {@link org.df4j.core.port.InpFlow}.
 */
public abstract class Actor extends AsyncProc {

    public Actor(Dataflow parent) {
        super(parent);
    }

    public Actor() {
        super();
    }

    @Override
    protected void run() {
        try {
            runAction();
            if (isCompleted()) {
                return;
            }
            this.awake(); // make loop
        } catch (Throwable e) {
            stop(e);
        }
    }

}
