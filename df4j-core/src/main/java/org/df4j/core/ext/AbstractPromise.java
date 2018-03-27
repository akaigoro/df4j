package org.df4j.core.ext;

import org.df4j.core.Actor;
import org.df4j.core.Port;
import org.df4j.core.Promise;

public abstract class AbstractPromise<T> extends Actor implements Promise<T> {
    protected final StreamInput<Port<T>> requests = new StreamInput<>();

    {
        useDirectExecutor();
    }

    public void postTo(Port<T> request) {
    	requests.post(request);
    }

}
