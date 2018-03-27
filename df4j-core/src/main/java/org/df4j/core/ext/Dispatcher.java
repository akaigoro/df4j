package org.df4j.core.ext;

import org.df4j.core.Port;

public class Dispatcher<T> extends AbstractPromise<T> {
    protected final StreamInput<T> resources = new StreamInput<>();

    public void postResource(T resource) {
    	resources.post(resource);
    }

	@Override
	protected void act() {
		T resource = resources.value;
		Port<T> request = requests.value;
		request.post(resource);
	}

}
