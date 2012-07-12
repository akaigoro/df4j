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

import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Request;

public class IORequest<R extends IORequest<R, C>, C> extends Request<R, Integer>
  implements CompletionHandler<Integer, C>, Runnable
{
    public static final AtomicInteger ids=new AtomicInteger(); // DEBUG

    public int rid=ids.addAndGet(1);
    protected C channel;
    protected ByteBuffer buffer;
    private boolean inRead;
    private boolean inTrans=false;

	public IORequest(ByteBuffer buffer) {
		this.buffer = buffer;
	}

    public void prepare(C channel, boolean read, Port<R> replyTo) {
        super.prepare(replyTo);
        this.channel = channel;
        this.inRead=read;
        if (read) {
            buffer.clear();
        } else {
            buffer.flip();
        }
        if (buffer.remaining()==0) {
            throw new IllegalArgumentException("no free space in the buffer");
        }
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
        forward();
    }

    private void checkInTrans() {
        if (inTrans) {
            throw new IllegalStateException("in transfer state already");
        }
        inTrans=true;
    }

	@Override
	public synchronized void completed(Integer result, C attachment) {
//        System.err.println(" IORequest.completed "+state+" rid="+rid);
        checkInTrans();
        if (inRead) {
            //System.out.println("channel read completed id="+id);
            buffer.flip();
        } else {
        	//System.out.println("channel write completed id="+id);
            buffer.clear();
        }
        inTrans=false;
        reply(result);
	}

    @Override
    public synchronized void failed(Throwable exc, C channel) {
        checkInTrans();
        this.exc=exc;
        inTrans=false;
        forward();
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
		return inRead;
	}

	public boolean isInTrans() {
        return inTrans;
	}
}
