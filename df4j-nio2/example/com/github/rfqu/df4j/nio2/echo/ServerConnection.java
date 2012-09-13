package com.github.rfqu.df4j.nio2.echo;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import com.github.rfqu.df4j.nio2.AsyncSocketChannel;
import com.github.rfqu.df4j.nio2.Connection;
import com.github.rfqu.df4j.nio2.SocketIORequest;

class ServerConnection extends Connection {
    private final EchoServer echoServer;
    AsyncSocketChannel channel;
    public int id;
    private ByteBuffer buffer;
    SerRequest request;
    boolean closed = false;

    public ServerConnection(EchoServer echoServer, AsyncSocketChannel channel) throws ClosedChannelException {
        this.echoServer = echoServer;
        this.channel = channel;
        this.id=echoServer.ids.addAndGet(1);
        buffer = ByteBuffer.allocate(EchoServer.BUF_SIZE);
        request = new SerRequest(buffer);
        channel.read(request, endRead);
    }

    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }
        try {
			channel.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			echoServer.conncClosed(this);
		}
    }

    SocketIOHandler<SerRequest> endRead = new SocketIOHandler<SerRequest>() {
        @Override
        protected void completed(int result, SerRequest request) {
            // System.out.println("  ServerRequest readCompleted id="+id);
            // read client's message as if all the data have been read
            buffer.position(buffer.limit());
            // write it back
            try {
                channel.write(request, endWrite);
            } catch (ClosedChannelException e) {
            }
        }

        @Override
        protected void closed(SerRequest request) {//throws IOException {
            ServerConnection.this.close();
        }
    };

    SocketIOHandler<SerRequest> endWrite = new SocketIOHandler<SerRequest>() {
        @Override
        protected void completed(int result, SerRequest request) {//throws IOException {
            // System.out.println("  ServerRequest writeCompleted id="+id);
            try {
                // System.out.println("  ServerRequest read started id="+id);
                channel.read(request, endRead);
            } catch (ClosedChannelException e) {
            	closed(request);
            }
        }

        @Override
        protected void closed(SerRequest request) {//throws IOException {
            ServerConnection.this.close();
        }
    };

    static class SerRequest extends SocketIORequest<SerRequest> {

        public SerRequest(ByteBuffer buf) {
            super(buf);
        }
    }
}