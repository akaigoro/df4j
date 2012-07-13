/**
 * Provides core classes for tagged actor programming.
 * Actors are not accessed directly by references, but with a key provided to
 * a demultiplexor. This allows:
 * <p>- create actors at the first access
 * <p>- load/store actors in persistent memory
 * <p>- create proxies to remote actors
 * <p>
 * Actors controlled by demultiplexors differ from ordinary actors: they consist of 2 parts - 
 * communication and data parts. Communication part holds incoming messages and data part
 * holds the state of the actor.
 * When non-existent actor is accessed, first communication part of the actor
 * is created, which holds message queue, and a request to create the data part is issued.
 * Communication part accumulates subsequent messages, and the actor starts running when request
 * to create data part is satisfied.
 * <p>
 * Two types of demultiplexors implemented: {@link com.github.rfqu.df4j.actordemux.ConservativeDemux}
 * and  {@link com.github.rfqu.df4j.actordemux.LiberalDemux}, which provide two kinds of
 * interaction between communication and data parts. Conservative interaction
 * resembles ordinary actors implemented by {@link com.github.rfqu.df4j.core.Actor}:
 * messages are subtypes of {@link com.github.rfqu.df4j.core.Link}, and data part 
 * must implement {@link com.github.rfqu.df4j.actordemux.Delegate} with computational method
 * {@link com.github.rfqu.df4j.actordemux.Delegate#act(Link message)}.
 * Liberal interaction gives more freedom to create various types of messages while preserving 
 * the structure of the data part: messages must extend {@link com.github.rfqu.df4j.actordemux.Action}
 * and  data part can be of any type. In particular, it can be of type byte[] representing
 * byte array just loaded from persistent storage and not deserialized into a java object.
 * Computational method belongs to the message.
 */
package com.github.rfqu.df4j.actordemux;