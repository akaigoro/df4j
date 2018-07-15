pipeline-nio: 
--------------
asyncronous network I/O, based on NIO2, provides 3 basic classes:

- ClientConnection: requests connection to the server and manages two queus of ByteBuffers, 
one for reading and another for writing. 

- ServerConnection: requests connection to a client and manages two queus of ByteBuffers, 
one for reading and another for writing. 

- AsyncServerSocketChannel - accepts ServerConnections, waits for the next client request
 and passes client to the ServerConnection