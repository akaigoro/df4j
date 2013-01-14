/*
 * Copyright 2011-2012 by Alexei Kaigorodov
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
package com.github.rfqu.df4j.nio2;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio.SocketIORequest;

/**
 * Wrapper over {@link AsynchronousSocketChannel}.
 * Simplifies input-output, handling queues of I/O requests.
 * 
 * Internally, manages 2 actors: one for reading requests and one for writing requests.
 * After request is served, it is sent to the port denoted by <code>replyTo</code> parameter in
 * the read/write methods.
 */
public class AsyncSocketChannel2 extends AsyncSocketChannel 
   implements CompletionHandler<Void, AsynchronousSocketChannel>
{
    protected volatile AsynchronousSocketChannel channel;
    {
        reader = new ReaderQueue();
        writer = new WriterQueue();
    }

    /**
     * for server-side socket
     * @param assch
     * @throws IOException 
     */
    public AsyncSocketChannel2(AsynchronousSocketChannel assch) {
        init(assch);
    }
    
    void init(AsynchronousSocketChannel attachement) {
        synchronized(this) {
            channel=attachement;
        }
        reader.resume();
        writer.resume();
        connEvent.post(new AsyncSocketChannel2(attachement));
    }
    
    /**
     * for client-side socket
     * Starts connection to a server.
     * IO requests can be queued immediately,
     * but will be executed only after connection completes.
     * If interested in the moment when connection is established,
     * add a listener. 
     * @throws IOException
     */
    public AsyncSocketChannel2(SocketAddress addr) throws IOException {
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        channel=AsynchronousSocketChannel.open(acg);
        channel.connect(addr, channel, this);
    }

    public void setTcpNoDelay(boolean on) throws IOException {
        channel.setOption(StandardSocketOptions.TCP_NODELAY, on);
    }
    
    public <R extends Callback<AsyncSocketChannel>> R addConnListener(R listener) {
    	connEvent.addListener(listener);
        return listener;
    }
    
    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public boolean isConnected() {
        return channel!=null;
    }

    public boolean isClosed() {
        return closed;
    }

    // ================== StreamPort I/O interface 

    /** disallows subsequent posts of requests; already posted requests 
     * would be processed.
     */
    @Override
    public void close() {
        closed=true;
        reader.close();
        writer.close();
    }

/*
    public synchronized void addListener(Callback<AsynchronousSocketChannel> listener) {
        connEvent.addListener(listener);
    }
*/
	//========================= backend
	
	/**
     * callback method for successful connection in client-side mode
     */
    @Override
    public void completed(Void result, AsynchronousSocketChannel channel) {
        init(channel);
    }

    /**
     * callback method for failed connection in client-side mode
     */
    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        connEvent.postFailure(exc);
    } 

    //===================== inner classes
    
    abstract class RequestQueue2 extends RequestQueue
    	implements CompletionHandler<Integer, SocketIORequest<?>>
    {
     protected Semafor channelAcc=new Semafor(); // channel accessible
     protected SocketIORequest<?> currentRequest;
     
     public RequestQueue2() {
        super(null); // immediate executor - act() method  
    }

     public void resume() {
         channelAcc.up();
     }
		//------------- CompletionHandler's backend
		
	 @Override
     public void completed(Integer result, SocketIORequest<?> request) {
		 currentRequest=null;
         channelAcc.up();
         request.post(result);
     }

     @Override
     public void failed(Throwable exc, SocketIORequest<?> request) {
         if (exc instanceof AsynchronousCloseException) {
             AsyncSocketChannel2.this.close();
         }
		 currentRequest=null;
         channelAcc.up();
         request.postFailure(exc);
     }
 }
	
    class ReaderQueue extends RequestQueue2 {
        //-------------------- Actor's backend
        
        @Override
        protected void act(SocketIORequest<?> request) throws Exception {
           currentRequest=request;
           if (request.isTimed()) {
               channel.read(request.getBuffer(),
                       request.getTimeout(), TimeUnit.MILLISECONDS, request, this);
           } else {
               channel.read(request.getBuffer(), request, this);
           }
        }
        
   		@Override
   		protected void complete() throws Exception {
   			completer.getReaderFinished().up();
   		}

    }
   	
    class WriterQueue extends RequestQueue2 {
        //-------------------- Actor's backend
        
        @Override
        protected void act(SocketIORequest<?> request) throws Exception {
        	currentRequest=request;
            if (request.isTimed()) {
                channel.write(request.getBuffer(), request.getTimeout(), TimeUnit.MILLISECONDS,
                        request, this);
            } else {
                channel.write(request.getBuffer(), request, this);
            }
        }
        
   		@Override
   		protected void complete() throws Exception {
   			completer.getWriterFinished().up();
   		}

    }
}
