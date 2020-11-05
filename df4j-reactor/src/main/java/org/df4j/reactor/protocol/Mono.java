package org.df4j.reactor.protocol;

import org.df4j.protocol.Scalar;

/**
 *  One-shot message with completion exceptions
 *
 * analogue of {@link Scalar}
 *
 * Consists of:
 * {@link reactor.core.publisher.Mono},
 * {@link org.reactivestreams.Subscriber}.
 *
 */
public class Mono {
    private Mono(){}
}
