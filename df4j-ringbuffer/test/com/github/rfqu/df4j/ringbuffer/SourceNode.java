package com.github.rfqu.df4j.ringbuffer;

import org.junit.Assert;

import com.github.rfqu.df4j.ringbuffer.BoltNode;
import com.github.rfqu.df4j.testutil.IntValue;
import com.github.rfqu.df4j.testutil.MessageSink;

/**
 * The intermediate passing node
 * 
 */
public class SourceNode extends BoltNode {
	/** tokens go there */
    public final Window<IntValue> output;
    int count;

    public SourceNode(MessageSink<Object> context) {
        output = new Window<IntValue>();
        super.setContext(context);
    }

    public void start(int count) {
        this.count=count;
        super.start();
    }

    @Override
    protected void act() {
        count--;
        if (count==0) {
            context.post(null);
            stop();
         } else {
             IntValue olv = output.get();
             olv.value=count;
             output.post(olv);
         }
    }
    
}