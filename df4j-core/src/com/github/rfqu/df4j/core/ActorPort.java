package com.github.rfqu.df4j.core;

/**
 * @param <M> type of accepted messages
 */
public interface ActorPort<M> extends StreamPort<M>, Callback<M> {

}
