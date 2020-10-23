package org.df4j.nio2.net.echo;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.nio2.net.ServerSocketPort;
import org.df4j.nio2.net.Connection;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * generates {@link EchoProcessor}s for incoming connections
 *
 */
public class EchoServer extends Actor {
    public static final int BUF_SIZE = 128;
    static final Logger LOG = LoggerFactory.getLogger(EchoServer.class);
    ServerSocketPort inp = new ServerSocketPort(this);
    Set<EchoProcessor> echoProcessors = new HashSet<>();
    long connBSerialNum = 0;

    public EchoServer(ActorGroup actorGroup, SocketAddress socketAddressdr) throws IOException {
        super(actorGroup);
        inp.connect(socketAddressdr, 2);
    }

    public EchoServer(SocketAddress socketAddressdr) throws IOException {
        this(new ActorGroup(), socketAddressdr);
    }

    public EchoServer(int port) throws IOException {
        this(new InetSocketAddress("localhost", port));
    }

    public void whenComplete() {
        LOG.info(" completion started");
        inp.cancel();
        for (EchoProcessor processor: echoProcessors) {
            processor.complete();
        }
    }

    public void whenError(Throwable e) {
        whenComplete();
    }

    @Override
    protected void runAction() throws Throwable {
        if (inp.isCompleted()) {
            complete();
            return;
        }
        Connection serverConn = inp.remove();
        LOG.info(" request accepted");
        EchoProcessor processor =
                new EchoProcessor(getDataflow(), serverConn, connBSerialNum++); // create client connection
        processor.start();
    }

    static class MyExec implements Executor {
        LinkedBlockingQueue<Runnable> q = new LinkedBlockingQueue<>();

        @Override
        public void execute(@NotNull Runnable command) {
            q.add(command);
        }

        public void doAll() throws InterruptedException {
            for (;;) {
                Runnable runnable = q.poll(1000, TimeUnit.SECONDS);
                if (runnable == null) {
                    break;
                }
                runnable.run();
            }
        }
    }

    public static void main(String... args) throws IOException, InterruptedException {
        int port = args.length==0?EchoTest.port:Integer.valueOf(args[0]);
        EchoServer server = new EchoServer(port);
        MyExec exec = new MyExec();
        server.setExecutor(exec);
        exec.doAll();
    }

    public static void startEcoServer(String... args) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        String userDir = System.getProperty("user.dir");
        String port = args.length==0?Integer.toString(EchoTest.port):args[0];
        pb.command("java",
                "-cp", userDir+"df4j-nio2/target/classes;"+userDir+"df4j-nio2/target/test-classes",
                EchoServer.class.getCanonicalName(), port);
        pb.start();
    }
}
