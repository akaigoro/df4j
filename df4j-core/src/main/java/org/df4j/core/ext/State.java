package org.df4j.core.ext;

import java.util.concurrent.Executor;

import org.df4j.core.Actor;

/**
 * represents a stage of a sequential execution process,
 * where stages are active one by one, and explicitly pass
 * control from one to another
 */
public abstract class State extends Actor {
    Semafor control = new Semafor(0);
    
    public State() {
	}

	public State(Executor executor) {
		super(executor);
	}

	public void start () {
    	control.up();
    }           
}