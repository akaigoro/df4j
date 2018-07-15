package org.df4j.nio2.net.echo;

import org.df4j.core.connector.messagescalar.ScalarCollector;
import org.df4j.nio2.net.AsyncSocketChannel;
import org.df4j.nio2.net.ServerConnection;

public class EchoServer extends ServerConnection {

    public EchoServer(ScalarCollector<AsyncSocketChannel> backport) {
        super("EchoServerConnection",backport);
        reader.output.subscribe(writer.input);
        writer.output.subscribe(reader.input);
        reader.injectBuffers(2, 128);
        LOG.config(getClass().getName()+" created");
    }
}
