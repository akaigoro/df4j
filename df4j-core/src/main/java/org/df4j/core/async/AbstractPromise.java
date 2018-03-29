package org.df4j.core.async;

import org.df4j.core.Actor;
import org.df4j.core.Port;

public abstract class AbstractPromise<T> extends Actor implements Promise<T> {
    protected final StreamInput<Port<T>> requests = new StreamInput<>();

    {
        useDirectExecutor();
    }

    public void postTo(Port<T> request) {
    	requests.post(request);
    }

    @Override
    protected final void act() {
        T resource = getValue();
        Port<T> request = requests.value;
        request.post(resource);
    }

    protected abstract T getValue();
}
