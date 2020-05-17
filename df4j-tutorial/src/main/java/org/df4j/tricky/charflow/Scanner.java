package org.df4j.tricky.charflow;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.port.InpChars;
import org.df4j.core.port.OutFlow;

public abstract class Scanner<T> extends Actor {
    public InpChars inp = new InpChars(this);
    public OutFlow<T> outp = new OutFlow<>(this);

}

