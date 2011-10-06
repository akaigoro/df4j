package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.MessageQueue;

public class AsyncServerSocketChannel extends AsyncChannel {
    ServerSocketChannel serverChannel;
    MessageQueue<AsyncSocketChannel> acceptors=new MessageQueue<AsyncSocketChannel>();

    public AsyncServerSocketChannel(InetSocketAddress listenAddr) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(listenAddr);
        serverChannel.configureBlocking(false);
        interestOn(SelectionKey.OP_ACCEPT);
    }

    /** offer from new AsyncServerSocket
     * 
     * @param acceptor
     * @return
     * @throws IOException
     * @throws ClosedChannelException
     */
    public synchronized void accept(AsyncSocketChannel acceptor) throws IOException {
        acceptors.enqueue(acceptor);
    }

    @Override
    void notify(SelectionKey key) {
        try {
            for (;;) {
                synchronized (acceptors) {
                    if (acceptors.isEmpty()) {
                        return;
                    }
                }
                if (serverChannel==null || !serverChannel.isOpen()) {
                    return;
                }
                SocketChannel channel = serverChannel.accept();
                if (channel==null) {
                    return;
                }
                AsyncSocketChannel acceptor;
                synchronized (acceptors) {
                    acceptor = acceptors.poll();
                }
                acceptor.connCompleted(channel);            
            }
        } catch (ClosedChannelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public ServerSocketChannel getChannel() {
        return serverChannel;
    }

    public void close() throws IOException {
//        interestOff(SelectionKey.OP_ACCEPT);
        serverChannel.close();
        serverChannel=null;
    }
    
}
