package org.df4j.core.ext;

import org.df4j.core.Actor;

public abstract class State extends Actor {
    Semafor control = new Semafor(0);
    
    public void start () {
    	control.up();
    }           
}