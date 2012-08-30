package com.github.rfqu.df4j.ioexample;

import java.nio.ByteBuffer;

import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.nio2.AsyncSocketChannel;
import com.github.rfqu.df4j.nio2.SocketIORequest;

class TracingRequest<R extends SocketIORequest<R>> extends SocketIORequest<R> {
    int readstarted, readfinished, writestarted, writefinished; 
	@Override
    public void prepare(AsyncSocketChannel channel, boolean read, Port<R> replyTo) {
        super.prepare(channel, read, replyTo);
        if (read) {
            readstarted++;
        } else {
            writestarted++;
        }
    }

    @Override
    public void prepare(AsyncSocketChannel channel, boolean read, Port<R> replyTo, long timeout) {
        super.prepare(channel, read, replyTo, timeout);
        if (read) {
            readstarted++;
        } else {
            writestarted++;
        }
    }

    @Override
    public synchronized void completed(Integer result, AsyncSocketChannel attachment) {
        super.completed(result, attachment);
        if (super.isReadOp()) {
            readfinished++;
        } else {
            writefinished++;
        }
    }

    @Override
    public synchronized void failed(Throwable exc, AsyncSocketChannel channel) {
        super.failed(exc, channel);
        if (super.isReadOp()) {
            readfinished++;
        } else {
            writefinished++;
        }
    }

    public TracingRequest(ByteBuffer buf) {
		super(buf);
	}    	
}