
package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.core.Task;

/**
 * Requires Java 5.
 */
public class EchoServer extends Thread {
    static final Logger log = Logger.getLogger( EchoServer.class ); 
    static final int nThreads=Runtime.getRuntime().availableProcessors();
    InetSocketAddress local9999;
    PrintStream out=System.out;
    PrintStream err=System.err;
    ExecutorService executor;
    AsyncSelector sel;
    AsyncServerSocketChannel assch;

    public EchoServer(int port) throws IOException {
        local9999 = new InetSocketAddress("localhost", port);
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
        log.setLevel(Level.INFO);
        executor = new SimpleExecutorService();
        /*
        ThreadFactoryTL tf=new ThreadFactoryTL();
        executor = Executors.newFixedThreadPool(nThreads, tf);
        tf.setExecutor(executor);
        */        
    }

    public void run() {
        try {
            Task.setCurrentExecutor(executor);
            out.println("Using " + executor.getClass().getCanonicalName());
            sel=new AsyncSelector(executor);
            assch=new AsyncServerSocketChannel(sel, local9999);
            new ServerConnection(assch);
            sel.run();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                assch.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                sel.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            executor.shutdownNow();
        }
    }
    
    class ServerConnection extends AsyncSocketChannel {
        AsyncServerSocketChannel s;
        SocketIORequest request=new SocketIORequest(4096, true);
        
        public ServerConnection(AsyncServerSocketChannel s) throws IOException {
            super(sel);
            this.s=s;
            super.connect(s);
        }

        @Override
        protected void connCompleted(SocketChannel channel) throws IOException {
            super.connCompleted(channel);
            try {
                new ServerConnection(s);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            read(request);
            log.debug("    server read started");
        }

        @Override
        protected void requestCompleted(SocketIORequest request) {
            super.requestCompleted(request);
            if (request.result!=null) {
                if (request.isReadOp()) {
                    if (request.result == -1) {
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
//                    log.debug("    ServerIOR.channel.close failed:");
                    e.printStackTrace();
                }
                if (!channel.isOpen()) {
                    log.debug("    server channel closed");
                    return;
                }
 //               log.debug("    ServerIOR "+(request.isReadOp()?"read":"write")+" Failed:");
                request.exc.printStackTrace();
            }
        }

    }

    public void close() {
        try {
            assch.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        executor.shutdownNow();
        log.info("Stopped");
    }

    public static void main(String[] args) throws Exception {
        int port=(args.length==0? 9998: Integer.parseInt(args[0])); 
        EchoServer t=new EchoServer(port);
        t.run();
    }
}
