package com.github.rfqu.df4j.ringbuffer;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.ringbuffer.BoltNode;
import com.github.rfqu.df4j.testutil.IntValue;

/**
 * The intermediate passing node
 * 
 */
public class TransformerNode extends BoltNode {
    int id;
	/** tokens come here */
    public final SinkPin<IntValue> input;
	/** tokens go there */
    public final SourcePin<IntValue> output;
	
    public TransformerNode(int id, int batchSize) {
        this.id=id;
    	output = new SourcePin<IntValue>(batchSize);
    	input = new SinkPin<IntValue>(batchSize);
    }

    public TransformerNode(int id, int batchSize, Callback<Object> context) {
        this(id, batchSize);
        super.setContext(context);
    }

	/**
     * the method to handle incoming messages for each received packet,
     * decrease the number of remaining hops. If number of hops become zero,
     * send it to sink, otherwise send to another node.
     */
	@Override
	protected void act() {
	    IntValue token = input.get(0);
        input.shift(1);
        if (input.isEmpty()) {
            input.flush();
        }
	    int remained = token.value;
        if (remained==0) {
            context.post(null);
            return;
        }

        token.value=remained-1;
        output.set(0, token);
        output.shift(1);
        if (output.isEmpty()) {
            output.flush();
        }
 	}
}