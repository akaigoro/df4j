package org.df4j.core.spi.reactivestream;

/**
 * A component that acts as both a Subscriber and Publisher.
 *
 * @param <T> the subscribed item type
 * @param <R> the published item type
 */
public interface Processor<T,R> extends Subscriber<T>, Publisher<R> {
}
