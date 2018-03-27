package org.df4j.core.ext;

import org.df4j.core.Actor;
import org.df4j.core.Port;

public class Dispatcher<T> extends Actor {
    protected final StreamInput<T> resources = new StreamInput<>();
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
