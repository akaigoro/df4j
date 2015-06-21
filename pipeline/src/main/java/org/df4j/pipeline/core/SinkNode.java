package org.df4j.pipeline.core;

import java.util.concurrent.Executor;
import org.df4j.pipeline.df4j.core.Actor;
import org.df4j.pipeline.df4j.core.Callback;
import org.df4j.pipeline.df4j.core.Port;
import org.df4j.pipeline.df4j.core.StreamPort;

/**
 * 
 * @author kaigorodov
 *
 * @param <I> type of input messages
 */
public abstract class SinkNode<I> extends Actor<I>
    implements Sink<I>
{
    public SinkNode() {
    }

    public SinkNode(Executor executor) {
        super(executor);
    }

    //----------------- Bolt part

    protected Callback<Object> context;

    @Override
    public void setContext(Callback<Object> context) {
        this.context = context;
    }

    public void start() {
    }

    public void stop() {
    }

    //----------------- Sink part

    @Override
    public StreamPort<I> getInputPort() {
        return this; // not input, as method post() can be overriden
    }

    /** there input messages return */
    private Port<I> returnPort;

    @Override
    public void setReturnPort(Port<I> returnPort) {
        this.returnPort=returnPort;
    }

    public void free(I item) {
        if (returnPort!=null) {
            returnPort.post(item);
        }
    }
    
    /*/-------------------------
	
    @Override
    protected void act() {
        if (input.isClosed()) {
            complete();
        } else {
            I message = input.get();
            act(message);
            free(message);
        }
    }

    protected void act(I message){}

    protected void complete(){}
    */
}