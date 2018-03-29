package org.df4j.core.async;

import org.df4j.core.Port;

public class CompletablePromise<T> extends AbstractPromise<T> implements Port<T> {
    protected final ConstInput<T> resource = new ConstInput<>();

    public void post(T resource) {
    	this.resource.post(resource);
    }

    protected T getValue() {
        return resource.get();
    }
}
