package org.df4j.core.ext;

import org.df4j.core.BaseActor;
import org.df4j.core.Port;

public class Dispatcher<T> extends BaseActor {
    protected final StreamInput<T> resources = new StreamInput<>();
    protected final StreamInput<Port<T>> requests = new StreamInput<>();

    public void postResource(T resource) {
    	resources.post(resource);
    }

    public void postRequest(Port<T> request) {
    	requests.post(request);
    }

	@Override
	protected void fire() {
		T resource = resources.value;
		Port<T> request = requests.value;
		request.post(resource);
	}

}
