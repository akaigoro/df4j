package org.df4j.pipeline.util;

import java.nio.CharBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import org.df4j.pipeline.core.SinkNode;

public class CharBufSink extends SinkNode<CharBuffer> {
    LinkedBlockingQueue<String> output=new LinkedBlockingQueue<String>();
    
    public CharBufSink() {
    }

    public CharBufSink(Executor executor) {
        super(executor);
    }

    public LinkedBlockingQueue<String> getOutput() {
		return output;
	}

	public boolean isClosed() {
        return input.isClosed();
    }
    
    //--------------- Dataflow backend
    
    StringBuilder sb=new StringBuilder();

    void formString() {
        String s=sb.toString();
        try {
            output.put(s);
        } catch (InterruptedException e) {
            // cannot happen for LinkedBlockingQueue
        }
        sb=new StringBuilder();
    }

    @Override
    protected void act(CharBuffer message) {
        CharBuffer inbuf=input.get();
        while (inbuf.hasRemaining()) {
            char c = inbuf.get();
            if (c=='\n') {
                formString();
            } else {
                sb.append(c);
            }
        }
        free(inbuf);
    }

    @Override
    protected void complete() {
        formString();
    }
}