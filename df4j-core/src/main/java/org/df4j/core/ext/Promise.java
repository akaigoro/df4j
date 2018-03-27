package org.df4j.core.ext;

import org.df4j.core.Actor;
import org.df4j.core.Port;

public class Promise<T> extends Actor {
    protected final ConstInput<T> resources = new ConstInput<>();
    protected final StreamInput<Port<T>> requests = new StreamInput<>();

    {
        useDirectExecutor();
    }

    public void postResource(T resource) {
    	resources.post(resource);
    }

    public void postRequest(Port<T> request) {
    	requests.post(request);
    }

	@Override
	protected void act() {
		T resource = resources.value;
		Port<T> request = requests.value;
		request.post(resource);
	}

}
