package org.df4j.pipeline.core;

/**
 * {@link mySinkPort} and {@link myreturnPort} must be initialized in subclasses
 * 
 * @author kaigorodov
 *
 * @param <I> type of input messages
 * @param <O> type of output messages
 */
public interface Transformer<I, O>  extends Sink<I>, Source<O> {
}