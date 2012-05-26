package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.StreamPort;

/**
 * Wrapper over AsynchronousSocketChannel.
 * Simplifies input-output, handling queues of requests.
 * @author rfq
 *
 */
public class AsyncSocketChannel extends Link {
    protected AsynchronousSocketChannel channel;
    protected boolean connected=false;
    protected boolean closed=false;
    protected Throwable connectionFailure=null;
    public final StreamPort<SocketIORequest> reader=new Reader();
    public final StreamPort<SocketIORequest> writer=new Writer();

    /**
     * for client-side socket
     * @throws IOException
     */
    public void connect(SocketAddress remote) throws IOException {
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        this.channel=AsynchronousSocketChannel.open(acg);
        channel.connect(remote, null, new CompletionHandler<Void, Object>() {
            @Override
            public void completed(Void result, Object attachment) {
                connCompleted();
            }
            @Override
            public void failed(Throwable exc, Object attachment) {
                connFailed(exc);
            } 
        });
    }

    /**
     * For server-side socket.
     */
	public void connect(AsyncServerSocketChannel assc) {
		assc.send(new AsyncServerSocketChannel.AcceptHandler() {
		    @Override
		    public void completed(AsynchronousSocketChannel result) {
//		        System.out.println("AsyncSocketChannel: connected to server");
		        channel=result;
                connCompleted();
		    }

		    @Override
		    public void failed(Throwable exc) {
		        connFailed(exc);
		    }

		});
	}

	protected void connCompleted() {
        connected=true;
        ((RequestQueue)reader).resume();
        ((RequestQueue)writer).resume();
    }

    protected void connFailed(Throwable exc) {
        //System.err.println("AsyncSocketChannel: connection failed:");
        connectionFailure=exc;
        //exc.printStackTrace();
    }

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public void close() throws IOException {
        closed=true;
        channel.close();
    }

    abstract class RequestQueue extends Actor<SocketIORequest> {
        protected BooleanPlace channelAcc=new BooleanPlace(); // channel accessible
        CompletionHandler<Integer, SocketIORequest> handler =
                new CompletionHandler<Integer, SocketIORequest>() {

                    @Override
                    public void completed(Integer result, SocketIORequest attachment) {
                        resume();
                        attachment.completed(result, AsyncSocketChannel.this);
                    }

                    @Override
                    public void failed(Throwable exc, SocketIORequest attachment) {
                        if (exc instanceof AsynchronousCloseException) {
                            try {
                                AsyncSocketChannel.this.close();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        } else {
                            resume();
                        }
                        attachment.failed(exc, AsyncSocketChannel.this);
                    }
            
        };
        protected void checkRequest(SocketIORequest request) {
            if (request==null) {
                throw new IllegalArgumentException("request==null");
            }
            if (connectionFailure!=null) {
                throw new IllegalStateException(connectionFailure);
            }
            if (closed) {
                throw new IllegalStateException("channel closed");
            }
        }

        protected void resume() {
            channelAcc.send();
        }
        
        protected void suspend() {
            channelAcc.remove();
        }

        @Override
        protected void complete() throws Exception {
            // TODO Auto-generated method stub
        }
    }
    
    class Reader extends RequestQueue {

        @Override
        public void send(SocketIORequest request) {
            checkRequest(request);
            request.startRead();
            super.send(request);
        }
        
        @Override
        protected void act(SocketIORequest request) throws Exception {
            suspend(); // block channel
//          System.out.println("channel read started id="+request.id);
            channel.read(request.buffer, request, handler);
        }
        
    }
    
    class Writer extends RequestQueue {

        @Override
        public void send(SocketIORequest request) {
            checkRequest(request);
            request.startWrite();
            super.send(request);
        }
        
        @Override
        protected void act(SocketIORequest request) throws Exception {
            suspend(); // block channel
//          System.out.println("channel read started id="+request.id);
            channel.write(request.buffer, request, handler);
        }
        
    }
    
}
