package org.df4j.core.ext;

import org.df4j.core.async.AbstractPromise;

public class Dispatcher<T> extends AbstractPromise<T> {
    protected final StreamInput<T> resources = new StreamInput<>();

    public void postResource(T resource) {
    	resources.post(resource);
    }

	protected T getValue() {
		return resources.get();
	}
}
