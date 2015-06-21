package org.df4j.core;

/**
 * 
 * T - type of tokens
 *
 */
public abstract class SharedPlace<T> extends Actor1<Port<T>> {
    
    public abstract void ret(T token);

}
