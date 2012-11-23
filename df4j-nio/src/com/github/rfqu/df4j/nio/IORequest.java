/* Copyright 2011-2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.github.rfqu.df4j.nio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Request;

/**
 * Request for an I/O operation.
 * @param <T> actual type of the request, after subclassing.
 */
public class IORequest<T extends IORequest<T>> extends Request<T, Integer> {
    public static final AtomicInteger ids=new AtomicInteger(); // DEBUG

    public int rid=ids.addAndGet(1);
    protected ByteBuffer buffer;
    private boolean inRead;
    private boolean inTrans=false;

	public IORequest(ByteBuffer buffer) {
		this.buffer = buffer;
	}

    public void prepareRead(Port<T> replyTo) {
        super.prepare(replyTo);
        this.inRead=true;
        buffer.clear();
        if (buffer.remaining()==0) {
            throw new IllegalArgumentException("no free space in the buffer");
        }
    }

    public void prepareWrite(Port<T> replyTo) {
        super.prepare(replyTo);
        this.inRead=false;
        buffer.flip();
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
    
    public void toIOCallback(IOCallback<T> handler) {
//      public void toCallback(IOCallback<IORequest<T>> handler) {
//        IORequest<T> r =  this;
        T r =  (T)this;
        if (exc == null) {
            if (result==-1) {
                handler.closed(r);
            } else {
                handler.completed(result, r);
            }
        } else {
            if (exc instanceof AsynchronousCloseException) {
                // System.out.println("  ServerRequest conn closed id="+id);
                handler.closed(r);
            } else if (exc instanceof InterruptedByTimeoutException) {
                handler.timedOut(r);
            } else {
                // System.out.println("  ServerRequest read failed id="+id+"; exc="+exc);
                handler.failed(exc, r);
            }
        }
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
    
    //======================== backend methods - called from socket handler 

	public synchronized void completed(Integer result) {
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

    public synchronized void failed(Throwable exc) {
        checkInTrans();
        this.exc=exc;
        inTrans=false;
        reply();
    }

    /** for timer */
//  @Override
  public synchronized void run() {
      reply();
  }

  private void checkInTrans() {
      if (inTrans) {
          throw new IllegalStateException("in transfer state already");
      }
      inTrans=true;
  }
  
}
