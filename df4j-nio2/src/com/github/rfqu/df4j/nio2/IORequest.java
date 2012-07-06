/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio2;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

public class IORequest<R extends IORequest<R, C>, C> extends Link 
  implements CompletionHandler<Integer, C>, Runnable
{
    public static final AtomicInteger ids=new AtomicInteger(); // DEBUG

    public int rid=ids.addAndGet(1);
    protected C channel;
    protected State state=State.FREE;
    protected ByteBuffer buffer;
    private Port<R> replyTo;
    protected Integer result;
    protected Throwable exc;
    private boolean inTrans=false;

	public IORequest(ByteBuffer buffer) {
		this.buffer = buffer;
	}

    public void prepare(C channel, boolean read, Port<R> replyTo2) {
        this.channel = channel;
        if (read) {
            state = State.READ;
            buffer.clear();
        } else {
            state = State.WRITE;
            buffer.flip();
        }
        this.replyTo = replyTo2;
        result = null;
        exc = null;
    }

    public void clear() {
//        checkInTrans();
        if (buffer!=null) {
            buffer.clear();
        }
    }

    /** for timer */
    @Override
    public synchronized void run() {
        switch (state) {
        case READ:
        case WRITE:
            state=State.TIMER;
            doReply();
            break;
        }
    }

    @SuppressWarnings("unchecked")
    private final void doReply() {
        if (replyTo!=null) {
            replyTo.send((R)this);
        }
    }
    
    private void checkInTrans() {
        if (inTrans) {
            throw new IllegalStateException("in transfer already");
        }
        inTrans=true;
    }

	@Override
	public synchronized void completed(Integer result, C attachment) {
//        System.err.println(" IORequest.completed "+state+" rid="+rid);
        checkInTrans();
        this.result=result;
        switch (state) {
        case READ:
            //System.out.println("channel read completed id="+id);
            buffer.flip();
            break;
        case WRITE:
            //System.out.println("channel write completed id="+id);
            buffer.clear();
            break;
        case TIMER:
            return;
        default:
            throw new IllegalStateException("IORequest.completed: in "+state+" already");
        }
        state=State.FREE;
        inTrans=false;
        doReply();
	}

    @Override
    public synchronized void failed(Throwable exc, C channel) {
        checkInTrans();
        switch (state) {
        case READ:
        case WRITE:
//            System.err.println("channel "+state+" failed rid="+rid+" exc="+exc);
            break;
        case TIMER:
            return;
        default:
            throw new IllegalStateException("IORequest.failed: in "+state+" already");
        }
        this.exc=exc;
        state=State.ERROR;
        inTrans=false;
        doReply();
    }

	public C getChannel() {
        return channel;
    }

    public void setBuffer(ByteBuffer buf) {
        this.buffer = buf;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

	public boolean isReadOp() {
		return state==State.READ;
	}

	public boolean isInTrans() {
        return state!=State.FREE;
	}

    public Integer getResult() {
        return result;
    }

	public Throwable getExc() {
		return exc;
	}

	public Port<R> getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(Port<R> replyTo) {
		this.replyTo = replyTo;
	}

	public enum State{FREE, READ, WRITE, TIMER, ERROR}
}
