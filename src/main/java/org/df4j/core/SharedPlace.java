package org.df4j.core;

/**
 * 
 * R - type of tokens
 *
 */
public abstract class SharedPlace<R> extends Actor1<Port<R>> {
    
    public abstract void ret(R token);

}
