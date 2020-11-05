package org.df4j.nio2.net.echo;

import ch.qos.logback.classic.Level;
import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.port.InpFlow;
import org.df4j.core.util.LoggerFactory;
import org.df4j.nio2.net.AsyncServerSocketChannel;
import org.df4j.nio2.net.Connection;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * generates {@link EchoProcessor}s for incoming connections
 *
 */
public class EchoServer extends Actor {
    public static final int BUF_SIZE = 128;
    protected final Logger LOG = LoggerFactory.getLogger(this, Level.INFO);
    InpFlow<Connection> inp = new InpFlow<>(this);
    AsyncServerSocketChannel connProducer;
    Set<EchoProcessor> echoProcessors = new HashSet<>();

    public EchoServer(ActorGroup dataflow, SocketAddress addr, int maxConnCount) throws IOException {
        super(dataflow);
        connProducer = new AsyncServerSocketChannel(dataflow, addr);
        connProducer.out.subscribe(inp);
        connProducer.release(maxConnCount);
        connProducer.start();
    }

    @Override
    protected void whenComplete(Throwable ex) {
        connProducer.onComplete(ex);
        for (EchoProcessor processor: echoProcessors) {
            processor.onComplete(ex);
        }
    }

    public void onComplete(Throwable ex) {
        complete(ex);
    }

    @Override
    protected void runAction() throws Throwable {
        if (inp.isCompletedExceptionally()) {
            complete(inp.getCompletionException());
            return;
        } else if (inp.isCompleted()) {
            complete();
            return;
        }
        Connection conn = inp.remove();
        EchoProcessor processor = new EchoProcessor(this, getActorGroup(), conn);
        echoProcessors.add(processor); 
        processor.start();
    }

}
