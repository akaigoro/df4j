package org.df4j.core.dataflow;

import org.df4j.core.port.InpFlood;

/**
 * Actor according to Carl Hewitt
 */
public abstract class SimpleActor<T> extends Actor {
    private InpFlood<T> inp = new InpFlood<>(this);

    public void onNext(T message) {
        inp.onNext(message);
    }

    public void onComplet() {
        inp.onComplete();
    }

    public void onError(Throwable t) {
        inp.onError(t);
    }

    @Override
    protected void runAction() throws Throwable {
        runAction(inp.remove());
    }

    protected abstract void runAction(T Message) throws Throwable;
}
