package com.github.rfqu.df4j.nio2.echo2;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

import com.github.rfqu.df4j.nio2.AsyncSocketChannel;
import com.github.rfqu.df4j.nio2.SocketIORequest;
import com.github.rfqu.df4j.nio2.echo.IOHandler;

class ServerConnection {
    static final int NBUFS=4;
    private final EchoServer echoServer;
    AsyncSocketChannel channel;
    public int id;
    boolean closed = false;

    public ServerConnection(EchoServer echoServer, AsynchronousSocketChannel channel2)
    //        throws ClosedChannelException
    {
        this.echoServer = echoServer;
        this.channel=new AsyncSocketChannel(channel2);
        this.id=echoServer.ids.addAndGet(1);
        for (int k=0; k<NBUFS; k++) {
            ByteBuffer buffer = ByteBuffer.allocate(EchoServer.BUF_SIZE);
            SerRequest request = new SerRequest(buffer);
            request.prepareRead(endRead);
            channel.send(request);
        }
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
			echoServer.connClosed(this);
		}
    }

    IOHandler<SerRequest> endRead = new IOHandler<SerRequest>() {
        @Override
		public void completed(int result, SerRequest request) {
            // System.out.println("  ServerRequest readCompleted id="+id);
            // read client's message as if all the data have been read
            request.getBuffer().position(request.getBuffer().limit());
            // write it back
            request.prepareWrite(endWrite);
            channel.send(request);
        }

        @Override
		public void closed(SerRequest request) {//throws IOException {
            ServerConnection.this.close();
        }
    };

    IOHandler<SerRequest> endWrite = new IOHandler<SerRequest>() {
        @Override
		public void completed(int result, SerRequest request) {//throws IOException {
            // System.out.println("  ServerRequest writeCompleted id="+id);
        	request.prepareRead(endRead);
            channel.send(request);
        }

        @Override
		public void closed(SerRequest request) {//throws IOException {
            ServerConnection.this.close();
        }
    };

    static class SerRequest extends SocketIORequest<SerRequest> {

        public SerRequest(ByteBuffer buf) {
            super(buf);
        }
    }
}