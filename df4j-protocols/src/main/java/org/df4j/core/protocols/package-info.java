/**
 *
 *  A protocol describes following properties of a connection:
 *
 *  - what kind of tokens are transferred through the connection: signals (pure events without value) or messages (events with value).
 *    In terms of Petri Nets, these are black and colored tokens, respectively.
 *
 *  - how many tokens can be transferred through the lifetime of the connection: one or many.
 *    Connections with single possible tokens are called scalar connections, and that token is called a scalar.
 *    Connections with many possible tokens are called streams.
 *
 *  - is backpressure supported. Backpressure has sense only for messages streams, because signals and scalars cannot cause memory exhausting.
 *
 *  {@link org.df4j.core.protocols.Flood} describes a unicast message stream without backpressure
 *  {@link org.df4j.core.protocols.Flow} describes a unicast message stream with backpressure.  It is copied fron JDK9.
 *  {@link org.df4j.core.protocols.SignalStream} describes a stream of signals. It can be unicast or multicast.
 *  {@link org.df4j.core.protocols.Scalar} describes a multicast scalar connection.
 *  It receives a single message, but then duplicates it for each subscriber.
 *
 */
package org.df4j.core.protocols;