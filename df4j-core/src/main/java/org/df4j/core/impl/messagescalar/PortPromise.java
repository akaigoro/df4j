package org.df4j.core.impl.messagescalar;

import org.df4j.core.impl.Actor;
import org.df4j.core.spi.messagescalar.Port;
import org.df4j.core.spi.messagescalar.Promise;

public class PortPromise<T> extends Actor implements Port<T>, Promise<T> {
    protected final ConstInput<T> resource = new ConstInput<>();
    protected final StreamInput<Port<T>> requests = new StreamInput<>();

    public void post(T resource) {
    	this.resource.post(resource);
    }

    public void postTo(Port<T> request) {
    	requests.post(request);
    }

    @Override
    protected final void act() {
        T resource = this.resource.get();
        Port<T> request = requests.value;
        request.post(resource);
    }
}
