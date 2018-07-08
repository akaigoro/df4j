package org.df4j.nio2.net.echo;

import org.df4j.core.connector.messagescalar.ScalarCollector;
import org.df4j.core.connector.permitstream.Semafor;
import org.df4j.core.connector.reactivestream.Publisher;
import org.df4j.core.connector.reactivestream.ReactiveOutput;
import org.df4j.core.connector.reactivestream.Subscriber;
import org.df4j.core.node.Action;
import org.df4j.core.node.AsyncAction;
import org.df4j.nio2.net.ServerConnection;

public class ConnectionSource extends AsyncAction implements Publisher<ServerConnection>, ScalarCollector<ServerConnection> {
    Semafor allowedConnections = new Semafor(this);
    ReactiveOutput<ServerConnection> output = new ReactiveOutput<>(this);
    int serialnum=0;

    public ConnectionSource(int connCount) {
        allowedConnections.release(connCount);
    }

    @Override
    public <S extends Subscriber<? super ServerConnection>> S subscribe(S subscriber) {
        output.subscribe(subscriber);
        return subscriber;
    }

    @Override
    public void post(ServerConnection message) {
        allowedConnections.release();
    }

    @Action
    protected void act() {
        ServerConnection conn = new EchoServerConnection(this);
        conn.name = "EchoServerConnection"+(serialnum++);
        output.post(conn);
    }
}
