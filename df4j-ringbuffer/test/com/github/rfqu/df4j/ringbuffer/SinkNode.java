package com.github.rfqu.df4j.ringbuffer;

import java.io.PrintStream;

import org.junit.Assert;

import com.github.rfqu.df4j.ringbuffer.BoltNode;
import com.github.rfqu.df4j.ringbuffer.BoltNode.Window;
import com.github.rfqu.df4j.testutil.IntValue;
import com.github.rfqu.df4j.testutil.MessageSink;

/**
 * Sink node
 * 
 */
public class SinkNode extends BoltNode {
    final static PrintStream err = System.err;

    /** tokens come here */
    public final Window<IntValue> input;
    long ilv;
    
    public SinkNode(MessageSink<Object> context) {
        input = new Window<IntValue>();
        super.setContext(context);
    }
    
    @Override
    protected void act() {
        IntValue olv = input.get();
        input.post(olv);
   }
}