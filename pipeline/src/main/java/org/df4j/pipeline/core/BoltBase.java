package org.df4j.pipeline.core;

public class BoltBase implements Bolt {
    protected Callback<Object> context;

    @Override
    public void setContext(Callback<Object> context) {
        this.context = context;
    }

    public void start() {
    }

    public void stop() {
    }
}