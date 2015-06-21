package org.df4j.pipeline.core;

import org.df4j.pipeline.df4j.core.Port;
import org.df4j.pipeline.df4j.core.StreamPort;

public interface Source<O>  extends Bolt {

    void setSinkPort(StreamPort<O> sinkPort);

    public Port<O> getReturnPort();

}
