
package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.io.*;

/**
 * Requires Java 5.
 */
public class EchoServer {
    static final Logger log = Logger.getLogger( EchoServer.class ); 
    static final int nThreads=Runtime.getRuntime().availableProcessors();
    InetSocketAddress local9999;
    ExecutorService executor;
    AsyncServerSocketChannel assch;

    public EchoServer(int port) throws IOException {
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
        log.setLevel(Level.INFO);
        executor = new SimpleExecutorService();
        /*
        ThreadFactoryTL tf=new ThreadFactoryTL();
        executor = Executors.newFixedThreadPool(nThreads, tf);
        tf.setExecutor(executor);
        */        
        log.info("Using " + executor.getClass().getCanonicalName());
        local9999 = new InetSocketAddress("localhost", port);
    }

    public void start() {
        executor.execute(new Runnable() {            
            @Override
            public void run() {
                try {
                    assch=new AsyncServerSocketChannel(local9999);
                    log.info("Listening " + local9999.toString());
                    new ServerConnection(assch);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }
    
    public void close() throws IOException {
        assch.close();
        executor.shutdownNow();
        log.info("Stopped");
    }
    
    class ServerConnection extends AsyncSocketChannel {
        AsyncServerSocketChannel s;
        SocketIORequest request=new SocketIORequest(4096, true);
        
        public ServerConnection(AsyncServerSocketChannel s) throws IOException {
            this.s=s;
            super.connect(s);
        }

        @Override
        protected void connCompleted(AsynchronousSocketChannel result) {
            super.connCompleted(result);
             try {
                new ServerConnection(s);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error("  connection failed:"+e.toString());
                return;
            }
            read(request);
            log.debug("    server read started");
        }

        @Override
        protected void requestCompleted(SocketIORequest request) {
            super.requestCompleted(request);
            if (request.getResult()!=null) {
                if (request.isReadOp()) {
                    if (request.getResult() == -1) {
                        log.debug("    server closing");
                        try {
                            super.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        return; // client has closed connection
                    }
                    ByteBuffer buffer=request.getBuffer();
                    buffer.position(buffer.limit()); // as if data was read and written back
                    super.write(request);
                } else {
                    ByteBuffer buffer=request.getBuffer();
                    buffer.clear();
                    super.read(request);
                }
            } else {
                // TODO gracefully shutdown when client closed connection
                try {
                    super.close();
                    return;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    log.error("    ServerIOR.channel.close failed:"+e);
                }
                if (!channel.isOpen()) {
                    log.debug("    server channel closed");
                    return;
                }
                log.error("    ServerIOR "+(request.isReadOp()?"read":"write")+" Failed:"+request.getExc().toString());
            }
        }

    }

    public static void main(String[] args) throws Exception {
        int port=(args.length==0? 9998: Integer.parseInt(args[0])); 
        EchoServer t=new EchoServer(port);
        t.start();
        Thread.sleep(Long.MAX_VALUE);
    }
}
