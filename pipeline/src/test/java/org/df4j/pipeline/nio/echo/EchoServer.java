package org.df4j.pipeline.nio.echo;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.df4j.pipeline.core.Pipeline;
import org.df4j.pipeline.core.SinkNode;
import org.df4j.pipeline.df4j.core.ListenableFuture;
import org.df4j.pipeline.io.net.AsyncServerSocketChannel;
import org.df4j.pipeline.io.net.AsyncSocketChannel;

/**
* To run tests, {@see EchoServerLockTest} and {@see EchoServerGlobTest}.
*/
public class EchoServer implements Closeable {
    public static final int defaultPort = 8007;
    public static final int BUF_SIZE = 8;//64;

    AsyncServerSocketChannel assch;
    Reactor reactor=new Reactor();
    Pipeline acceptor = new Pipeline();
    
    public EchoServer(SocketAddress addr, int maxConn) throws IOException {
        assch=new AsyncServerSocketChannel(addr, maxConn);
        acceptor.setSource(assch).setSink(reactor);
    }

    public ListenableFuture<Object> start() {
        acceptor.start();
        return acceptor;
   }

    public void close() {
        acceptor.stop();
        assch.close();
    }

    public ListenableFuture<Object> getFuture() {
        return acceptor;
    }

    /**
     * accepted connections, formatted as {@link AsyncSocketChannel}, arrive to {@link myInput}.
     * For each connection, echoing pipline is created.
     */
    static class Reactor extends SinkNode<AsyncSocketChannel> {

        /** creates a pipeline which serves one client, echoing input packets
         */
        @Override
        protected void act(AsyncSocketChannel channel) {
            channel.reader.injectBuffers(2, BUF_SIZE);
            Pipeline pipeline = new Pipeline();
            pipeline.setSource(channel.reader).setSink(channel.writer);
            pipeline.start();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("classPath=" + System.getProperty("java.class.path"));
        Integer port;
        if (args.length >= 1) {
            port = Integer.valueOf(args[0]);
        } else {
            port = defaultPort;
        }
        int maxConn;
        if (args.length >= 2) {
            maxConn = Integer.valueOf(args[1]);
        } else {
            maxConn = 1000;
        }
        SocketAddress addr = new InetSocketAddress("localhost", port);
		
        System.out.println("EchoServer at "+addr+" started");
        EchoServer es = new EchoServer(addr, maxConn);
		ListenableFuture<Object> future = es.start();
        future.get();

        // inet addr is free now
        System.out.println("EchoServer finished");
    }
}
