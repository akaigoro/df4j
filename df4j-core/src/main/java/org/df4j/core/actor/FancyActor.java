package org.df4j.core.actor;

import org.df4j.core.actor.Actor;
import org.df4j.core.util.ActionCaller;
import org.df4j.core.util.invoker.Invoker;

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
