package org.df4j.pipeline.core;

import org.df4j.pipeline.df4j.core.Port;
import org.df4j.pipeline.df4j.core.StreamPort;

public interface Sink<I> extends Bolt {

    public void setReturnPort(Port<I> returnPort);

    public StreamPort<I> getInputPort();

}
