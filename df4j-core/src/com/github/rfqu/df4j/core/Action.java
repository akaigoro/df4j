package com.github.rfqu.df4j.core;

/** Active message. Contains logic to act on actor's state.
 * 
 * @author kaigorodov
 *
 * @param <Handler>
 */
public abstract class Action<Handler> extends Link {
    public abstract void act(Handler p);
}