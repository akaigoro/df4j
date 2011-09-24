package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Actor;

/*
 *  final AsynchronousServerSocketChannel listener =
      AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(5000));

  listener.accept(null, new CompletionHandler<AsynchronousSocketChannel,Void>() {
      public void completed(AsynchronousSocketChannel ch, Void att) {
          // accept the next connection
          listener.accept(null, this);

          // handle this connection
          handle(ch);
      }
      public void failed(Throwable exc, Void att) {
          ...
      }
  });
 */
public abstract class AsyncServerSocketChannel extends AsyncChannel {
    AsynchronousServerSocketChannel channel;
    
    public AsyncServerSocketChannel() throws IOException {
        channel=open();
    }

    public AsynchronousServerSocketChannel getChannel() {
        return channel;
    }
    
    public void start() {
        channel.accept(this, acceptCompletion);
    }
    
    protected void completed(AsynchronousSocketChannel ch) {
        // accept the next connection
        channel.accept(this, acceptCompletion);

        // handle this connection
        accepted(ch);
   }

    public void failed(Throwable exc) {
        // TODO Auto-generated method stub
        if (exc instanceof AsynchronousCloseException) {
            return; // just closed
        }
        System.err.println("accept failed:");
        exc.printStackTrace();
    }
    

    public abstract void accepted(AsynchronousSocketChannel ch);
    
    public static AsynchronousServerSocketChannel open() throws IOException {
        ExecutorService executor=Actor.getCurrentExecutor();
        AsynchronousChannelGroup acg=getGroup(executor);
        return AsynchronousServerSocketChannel.open(acg);
    }
    
    static CompletionHandler<AsynchronousSocketChannel, AsyncServerSocketChannel> acceptCompletion
        = new CompletionHandler<AsynchronousSocketChannel, AsyncServerSocketChannel>()
    {
        @Override
        public void completed(AsynchronousSocketChannel result, AsyncServerSocketChannel assc) {
            assc.completed(result);
        }
        @Override
        public void failed(Throwable exc, AsyncServerSocketChannel assc) {
            assc.failed(exc);
        }
    };
}
