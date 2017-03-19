package org.df4j.core.ext;

import org.df4j.core.Actor;
import org.df4j.core.Port;

public class Dispatcher<T> extends Actor {
    protected StreamInput<T> resources = new StreamInput<>();
    protected StreamInput<Port<T>> requests = new StreamInput<>();
    
    public void postResource(T resource) {
    	resources.post(resource);
    }

    public void postRequest(Port<T> request) {
    	requests.post(request);
    }

	@Override
	protected void act() throws Exception {
		T resource = resources.value;
		Port<T> request = requests.value;
		request.post(resource);
	}

}
