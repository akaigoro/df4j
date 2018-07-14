package org.df4j.nio2.net.echo;

import org.df4j.core.connector.messagescalar.ScalarCollector;
import org.df4j.nio2.net.AsyncSocketChannel;
import org.df4j.nio2.net.ServerConnection;

public class EchoServerConnection extends ServerConnection {

    public EchoServerConnection(ScalarCollector<AsyncSocketChannel> backport) {
        super(backport);
        reader.output.subscribe(writer.input);
        writer.output.subscribe(reader.input);
        reader.injectBuffers(2, 128);
        LOG.config(getClass().getName()+" created");
    }
}
