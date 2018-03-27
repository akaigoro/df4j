package org.df4j.core.ext;

import org.df4j.core.Port;

public class CompletablePromise<T> extends AbstractPromise<T> {
    protected final ConstInput<T> resources = new ConstInput<>();

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
