package main.java.org.df4j.fancy;

import main.java.org.df4j.fancy.invoker.Invoker;
import org.df4j.core.actor.Actor;

/**
 * Actor is a reusable AsyncProc: after execution, it executes again as soon as new array of arguments is ready.
 */
public abstract class FancyActor extends Actor {

    protected Invoker actionCaller;

    protected void runAction() throws Throwable {
        if (actionCaller == null) {
            actionCaller = ActionCaller.findAction(this, getParamCount());
        }
        Object[] args = collectArgs();
        actionCaller.apply(args);
    }

}
