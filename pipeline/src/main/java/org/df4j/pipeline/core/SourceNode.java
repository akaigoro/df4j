package org.df4j.pipeline.core;

import java.util.concurrent.Executor;
import org.df4j.pipeline.df4j.core.StreamPort;

/**
 * 
 * @author kaigorodov
 *
 * @param <O> type of output messages
 */
public abstract class SourceNode<O> extends BoltNode
    implements Source<O>
{
    //----------------- Source part

    /** there output messages go */
    protected StreamPort<O> sinkPort;
    
    @Override
    public void setSinkPort(StreamPort<O> sinkPort) {
        this.sinkPort=sinkPort;
    }
    
    //-------------------------

	public SourceNode() {
    }

    public SourceNode(Executor executor) {
        super(executor);
    }
}