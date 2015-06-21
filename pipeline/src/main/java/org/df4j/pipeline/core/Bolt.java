package org.df4j.pipeline.core;

public interface Bolt {

    public void setContext(Callback<Object> context);
    
    public void start();

    public void stop();

}
