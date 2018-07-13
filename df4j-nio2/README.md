pipeline-nio: 
--------------
asyncronous network I/O, based on NIO2, provides two basic classes:

 - AsyncServerSocketChannel - implements Source<AsyncSocketChannel>. 
Generates accepted connections to clients on server side.

 - AsyncSocketChannel represents a network connection and contains 2 objects:
 -- Source<ByteBuffer> reader - to be used as the first node in a network-connected pipeline 
 -- Sink<ByteBuffer> writer - to be used as the last node in a network-connected pipeline 

AsyncSocketChannel can be used both on client and server sides.
