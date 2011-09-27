
   /**
   *   Java Network Programming, Third Edition
   *   By Elliotte Rusty Harold
   *   Third Edition October 2004
   *   ISBN: 0-596-00721-3
   */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Iterator;
import java.net.*;

public class NonblockingSingleFileHTTPServer {

    private ByteBuffer contentBuffer;
    private int port = 80;

    public NonblockingSingleFileHTTPServer(ByteBuffer data, String encoding, String MIMEType, int port) throws UnsupportedEncodingException {

        this.port = port;
        String header = "HTTP/1.0 200 OK\r\n" + "Server: OneFile 2.0\r\n" + "Content-length: " + data.limit() + "\r\n" + "Content-type: " + MIMEType + "\r\n\r\n";
        byte[] headerData = header.getBytes("ASCII");

        ByteBuffer buffer = ByteBuffer.allocate(data.limit() + headerData.length);
        buffer.put(headerData);
        buffer.put(data);
        buffer.flip();
        this.contentBuffer = buffer;

    }

    public void run() throws IOException {

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverChannel.socket();
        Selector selector = Selector.open();
        InetSocketAddress localPort = new InetSocketAddress(port);
        serverSocket.bind(localPort);
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {

            selector.select();
            Iterator keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();
                keys.remove();
                try {
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel channel = server.accept();
                        channel.configureBlocking(false);
                        SelectionKey newKey = channel.register(selector, SelectionKey.OP_READ);
                    } else if (key.isWritable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        if (buffer.hasRemaining()) {
                            channel.write(buffer);
                        } else { // we're done
                            channel.close();
                        }
                    } else if (key.isReadable()) {
                        // Don't bother trying to parse the HTTP header.
                        // Just read something.
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(4096);
                        channel.read(buffer);
                        // switch channel to write-only mode
                        key.interestOps(SelectionKey.OP_WRITE);
                        key.attach(contentBuffer.duplicate());
                    }
                } catch (IOException ex) {
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException cex) {
                    }
                }
            }
        }
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Usage: java NonblockingSingleFileHTTPServer file port encoding");
            return;
        }

        try {
            String contentType = "text/plain";
            if (args[0].endsWith(".html") || args[0].endsWith(".htm")) {
                contentType = "text/html";
            }

            FileInputStream fin = new FileInputStream(args[0]);
            FileChannel in = fin.getChannel();
            ByteBuffer input = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());

            // set the port to listen on
            int port;
            try {
                port = Integer.parseInt(args[1]);
                if (port < 1 || port > 65535)
                    port = 80;
            } catch (Exception ex) {
                port = 80;
            }

            String encoding = "ASCII";
            if (args.length > 2)
                encoding = args[2];

            NonblockingSingleFileHTTPServer server = new NonblockingSingleFileHTTPServer(input, encoding, contentType, port);
            server.run();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println(ex);
        }

    }

}
