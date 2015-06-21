package org.df4j.pipeline.core;

import java.util.concurrent.Executor;
import org.df4j.core.Actor;

/**
 * @author kaigorodov
 */
public abstract class BoltNode extends Actor
    implements Bolt
{
    protected Callback<Object> context;
    protected Lockup lockUp = new Lockup();

    public BoltNode() {
    }

    public BoltNode(Executor executor) {
        super(executor);
    }

    public void setContext(Callback<Object> context) {
        this.context = context;
    }

    public void start() {
        lockUp.on();
    }

    public void stop() {
        lockUp.off();
    }

    @Override
    protected void handleException(Throwable exc) {
        context.postFailure(exc);
    }
}