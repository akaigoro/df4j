package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.Link;

public class AsyncServerSocketChannel 
extends Actor<AsyncServerSocketChannel.AcceptHandler> {
    private AsynchronousServerSocketChannel channel;
    private BooleanPlace channelAcc=new BooleanPlace(); // channel accessible   
    public AsyncServerSocketChannel() throws IOException {
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        channel=AsynchronousServerSocketChannel.open(acg);
    }

    public AsynchronousServerSocketChannel getChannel() {
        return channel;
    }
    /*
    public static AsynchronousServerSocketChannel open() throws IOException {
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        return AsynchronousServerSocketChannel.open(acg);
    }
*/
    @Override
    protected void act(AcceptHandler connection) throws Exception {
        channelAcc.remove();
        channel.accept(connection, handler);
    }

    @Override
    protected void complete() throws Exception {
        channel.close();
    }
    
    CompletionHandler<AsynchronousSocketChannel,AcceptHandler> handler
        = new CompletionHandler<AsynchronousSocketChannel,AcceptHandler>() {

        @Override
        public void completed(AsynchronousSocketChannel result, AcceptHandler attachment) {
            channelAcc.send();
            attachment.completed(result);
        }

        @Override
        public void failed(Throwable exc, AcceptHandler attachment) {
            channelAcc.send();
            attachment.failed(exc);
        }

    };
    
    static abstract class AcceptHandler extends Link {
        public abstract void failed(Throwable exc);
        public abstract void completed(AsynchronousSocketChannel result);        
    }
}
