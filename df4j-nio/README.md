Project df4j-nio provides common interface for both NIO1 (using Selectors)
and NIO2 (using Asynchronous Channels, requires java 7 and above).
Implementations reside in projects df4j-nio1 and df4h-nio2, respectively.
One of these projects should be accessible in the current class path.
Acess to accessible implementation is provided by the class AsyncChannelFactory.
If both implementations are present, AsyncChannelFactory selects NIO2 implementation.

Two main classes are AsyncServerSocketChannel and AsyncSocketChannel. The first one is used only
accepts incoming connections at server side. The latter performs two-way input-output over established
connections on both sides.

An instance of AsyncChannelFactory itself can be got in this way:
<pre>
    AsyncChannelFactory asyncChannelFactory=AsyncChannelFactory.getCurrentAsyncChannelFactory();
</pre>

If it is known which implementation should be used, it can be created directly:
<pre>
    AsyncChannelFactory asyncChannelFactory=new AsyncChannelFactory1();
</pre>

A sketch of a server program is as follows:
<pre>
    // initiate server
    AsyncServerSocketChannel assc=asyncChannelFactory.newAsyncServerSocketChannel();
    assc.bind(socketAddress);
    
    // accept connections, synchronous way
    while (true) {
        ListenableFuture&lt;AsyncSocketChannel&gt; connectionEvent=assc.accept();
        // following operation blocks, waiting for the next client connection request
        AsyncSocketChannel asc = connectionEvent.get();
    }
</pre>

Since ListenableFuture provides also asynchronous notifications, it is possible to 
make a non-blocking acception loop:
<pre>
    class Acceptor implements Callback&lt;AsyncSocketChannel&gt; {
        void next() {
            ListenableFuture&lt;AsyncSocketChannel&gt; connectionEvent=assc.accept();
            connectionEvent.addListener(this); // this operation does not block
        }
        
        @Override
        public void post(AsyncSocketChannel readyChannel) {
            // do something with the newly created AsyncSocketChannel
            ...
            next();// wait for the next client
        }
        
        @Override
        public void postFailure(Throwable exc){
            // react to possible I/O errors
        }
    }
    
    Acceptor acceptor=new Acceptor();
    acceptor.next();
</pre>

Note that in both variants the server does not automatically accepts all incoming connection requests.
After each accepted connection, programmer explicitly initiates next connection.

Client-side connection is established like this:
<pre>
    AsyncSocketChannel asc=asyncChannelFactory.newAsyncSocketChannel();
    asc.connect(addr);
</pre>

Here connection is created but not completed, and actual connection monent
can be observed via ListenableFuture returned by asc.getConnEvent(). But not yet connected
AsyncSocketChannel can accept I/O requests right after creation (requests will be buffered).

Real I/O exchange is performed using instances of class SocketIORequest:
<pre>
    ByteBuffer buf == .. // obtained in any way
    SocketIORequest r=new SocketIORequest(buf);
    asc.read(r);
    // or
    asc.write(r); // read and write operations does not block
</pre>

Read and write operations are performed asynchronousely. SocketIORequest implements Future, so user can wait for 
completion of I/O operation:
<pre>
    SocketIORequest r=new SocketIORequest(buf);
    asc.read(r);
    int res=r.get(); // returns actual number of bytes read
</pre>

SocketIORequest also implements Promise&lt;SocketIORequest&gt;, so it is possible to react asyncronousely:
<pre>
    /** reads specified amount of bytes from a TCP connection
     *  and stores them in an array
     */
    class DataLoader implements Port&lt;SocketIORequest&gt; {
        AsyncSocketChannel asc;
        SocketIORequest r=new SocketIORequest(ByteBuffer.allocate(1024));
        byte[] data;
        int loaded=0;
        CompletableFuture&lt;byte[]&gt; result=new CompletableFuture&lt;byte[]&gt();;
        
        DataLoader(AsyncSocketChannel asc, int nbytes) {
            this.asc = asc;
            r=new SocketIORequest(ByteBuffer.allocate(1024));
            data=new byte[nbytes];
        }
        
        /** starts reading operation */
        ListenableFuture read() {
            int remained=data.length-loaded;
            if (remained == 0) {
                // all bytes have been read
                result.post(data); // notify listeners
                return result;
            }
            if (remained &lt; r.getBuffer().remaining()) {
                // shrink buffer
                r.getBuffer().limit(remained);
            }
            asc.read(r);            
            r.addListener(this); // DataLoader.post() will be invoked
            return result;
        }
        
        /** handles the end of reading operation */
        @Override
        public void post(SocketIORequest r) {
            if (r.getException()!=null) {
               // reading failed
               result.postFailure(r.getException()); // inform listeners
               return;
            }
            int bytesRead=r.getResult(); // must be positive
            r.getBuffer().get(data, loaded, bytesRead);
            loaded+=bytesRead;
            read(); // read next portion
        }
    }
    
    DataLoader loader=new DataLoader(asc, 100500);
    ListenableFuture result=loader.read();  // start non-blocking read
    ....
    byte array[]=result.get();// waits if nessesary
    // or set a callback to be invoked when all data will be read:
    result.addListener(new Callback&lt;byte[]&gt;() {
        public void post(byte[] array) {
            // do something with the array
        }
        public void postFailure(Throwable exc){
            // react to possible I/O errors
        }
    });
</pre>


