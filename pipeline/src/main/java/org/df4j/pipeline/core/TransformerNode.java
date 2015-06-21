package org.df4j.pipeline.core;

import java.util.concurrent.Executor;
import org.df4j.pipeline.df4j.core.StreamPort;

/**
 * 
 * @author kaigorodov
 *
 * @param <I> type of input messages
 * @param <O> type of output messages
 */
public abstract class TransformerNode<I, O> extends SinkNode<I>
    implements Transformer<I, O>
{
    public TransformerNode() {
    }

    public TransformerNode(Executor executor) {
        super(executor);
    }
    //----------------- Source part

    /** there output messages go */
    protected StreamPort<O> sinkPort;
    
    @Override
    public void setSinkPort(StreamPort<O> sinkPort) {
        this.sinkPort=sinkPort;
    }
}