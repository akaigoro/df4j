package org.df4j.core.communicator;

import org.df4j.protocol.Signal;

/**
 * {@link Trigger} can be considered as a one-shot AsyncSemaphore: once released, it always satisfies aquire()
 */
public class Trigger extends SignalSubscribers<Signal.Subscriber> implements Signal.Publisher, Signal.Subscriber {
}
