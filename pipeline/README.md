The project is in alpha-stage. Be ready to encounter bugs. Suggest more codec subprojects
to be implemented in first place.

Pipeline is a linear chain of dataflow nodes with additional rule: after processing,
messages are returned to the sender.
Senders consider messages as a resource and stop working when receivers do not return messages.
This prevents memory overflow when receivers are slow but senders continue to produce messages.
Senders implement Source interface, receivers implement Sink interface, and internmetiate nodes
implement Transformer interface, which is simply a combination of both. All interfaces are parameterized with types
they can send or receive.  So, typical pipeline looke like:

Source<T1> => Transformer<T1,T2> => Transformer<T2,T3> => Sink<T3>

All nodes in the pipeline can perform in parallel,
provided that they are created with parallel-capable Executor.
 
All subprojects rely on [df4j-core](https://github.com/rfqu/df4j/tree/master/df4j-core) project.

Subprojects:
------------

pipeline-core: basic implementations of Source and Sync interfaces. 

pipeline-nio: asyncronous network I/O, based on NIO2, provides two basic classes:

 - AsyncServerSocketChannel - implements Source<AsyncSocketChannel>. 
Generates accepted connections to clients on server side.

 - AsyncSocketChannel represents a network connection and contains 2 objects:
 -- Source<ByteBuffer> reader - to be used as the first node in a network-connected pipeline 
 -- Sink<ByteBuffer> writer - to be used as the last node in a network-connected pipeline 

AsyncSocketChannel can be used both on client and server sides.

codec* projects
---------------

provide popular coding and encoding procedures.
All codecs can be used in asynchronous (push) way, and some also in synchronous (pull) way.

codec-charbyte: coding and decoding streams of character buffers in/from streams of byte buffers.

codec-json: reads and writes JSON files

codec-javon: reads and writes Javon files. Javon ia an extension to JSON,
adding true java-like objects, while JSON objects still are converted to java.util.Maps.

codec-xml
---------
in progress

Motivation
----------
Create a library which is able to replace Netty in many projects but is an order of magnitude smaller.
A couple of facts about Netty:

- volume of source code to build minimal echo server is about 2 megabytes
- 82 classes and interfaces implement/extend io.netty.util.concurrent.Future interface.

