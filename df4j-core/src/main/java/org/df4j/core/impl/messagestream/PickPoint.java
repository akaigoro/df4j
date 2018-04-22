package org.df4j.core.impl.messagestream;

import org.df4j.core.impl.messagestream.Actor1;
import org.df4j.core.spi.messagescalar.Port;
import org.df4j.core.spi.messagescalar.Promise;

/**
 * A post box.
 * Producers leave parcells via {@link #post(Object)} method.
 * Consumers subscribe asynchronously via {@link #postTo(Port)} method.
 * After consumer have received parcel, it is unsubscribed and,
 * if it wants to receive another parcel, then it needs to subscribe once more.
 * @param <R>
 */
public class PickPoint<R> extends Actor1<R> implements Promise<R> {
	protected final StreamInput<Port<R>> requests = new StreamInput<>();

	public void postTo(Port<R> request) {
		requests.post(request);
	}

    @Override
    protected void act(R resource) throws Exception {
        Port<R> request = requests.value;
        request.post(resource);
    }
}
