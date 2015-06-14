package org.df4j.core.ext;

import org.df4j.core.Listenable;
import org.df4j.core.Listener;
import org.df4j.core.Actor;
import org.df4j.core.Actor.PinBase;
import org.df4j.core.func.Promise;

/**
 * Demonstrates declaration of custom {@link Pin} 
 *
 * This pin carries demand(s) of the result.
 * Demand is two-fold: it is an input pin, so firing possible only if
 * someone demanded the execution, and it holds listeners' ports where
 * the result should be sent. 
 * @param <R>  type of result
 */
public class Demand<R> extends PinBase<Listener<R>>
    implements Listenable<R>, Listener<R>
{
    private Promise<R> listeners=new Promise<R>();

    public Demand(Actor actor) {
        super(actor);
    }

    /** indicates a demand
     * @param sink Port to send the result
     * @return 
     */
    @Override
    public Listenable<R> addListener(Listener<R> sink) {
        checkOn(sink);
        return this;
    }

    @Override
    protected boolean turnedOn(Listener<R> sink) {
        listeners.addListener(sink);
        return true;
    }

    /** satisfy demand(s)
     */
    @Override
    public void post(R m) {
        listeners.post(m);
    }

    @Override
    public void postFailure(Throwable exc) {
        listeners.postFailure(exc);
    }
}