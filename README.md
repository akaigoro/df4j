# Data Flow For Java

Simplicity is prerequisite for reliability. - Edsger W. Dijkstra
------------------------------------------

df4j is an abbreviation of "Data Flow for Java".
It is a library to support asynchronous computations of all flavours: futures, promises, asynchronous procedure calls, actors, reactive streams.

For those interested in the history of dataflow programming, I recommend to start with the [definition in Wikipedia](https://en.wikipedia.org/w/index.php?title=Dataflow_programming)
and then short introductory article "Dataflow Programming: Concept, Languages and Applications" by Tiago Boldt Sousa.

The primary goal of this library is to investigate the anatomy of asynchronous programming.
So this project avoids highly optimized cryptic code usually found in such libraries. The main goal is to make readable code.

The asynchronous programming always attracted Java programmers,
and the absence of a complete asynchronous support in language and runtime only stimulated programmers to find their own solutions.
Today some asynchronous libraries for Java are very popular, e.g. RxJava, vert.x, Akka.
However, all they imply steep learning curve and hides implementation details under the hood.
Df4j ia an attempt to discover the basic building elements of asynchronous computations,
and allow developer to freely combine those elements, and add new ones.
It resembles children's building kit: a set of small parts which can be connected together and be assembled in arbitrary complex constructs.

The design of Df4j is built on following foundation principles:

1. Any asynchronous computation can be represented as a (dataflow) graph, which consists of active nodes and nested dataflow graphs.
Such a tree structure allows exceptions propagate from leafs to the root graph, and to watch exceptions only at the root node.

2. Active node, in turn,  consists of:
 - ports: asynchronous input and output parameters. Each port is a (relatively complex) object.
 - user-defined computational procedure,
 - reference to an Executor, and
 - an object that glues all that components together, usually a descendant of class org.df4f.core.dataflow.AsyncProc.  
 
3. Each port has 2 states: ready and blocked. Input port is ready when it has received a token. 
Output port is ready when it has room to store a new token.
Output ports for signals are always ready, as storing signals requires only a counter.
Similary, output ports for scalar messages are always ready. 
The node is submitted to the attached executor when all its ports become ready.
Then the user-defined computational procedure is executed.

4. Nodes are interconnected so that output port of one node is connected to an input port of another node. 
Connected ports must support the same communication protocol.
Some ports may support more than one protocol.

Currently Df4j has ports for following protocols:

1. Signal flow. This is the asynchronous variant of the protocol used by java.util.concurrent.Semaphore. 
2. Scalar messages: this is the protocol similar to that used by java.util.concurrent.CompletableFuture. At most one message or an error is sent.
3. Unbound message streams, without backpressure. Backpressure can be added later using permit stream and can connect far standing nodes.
4. Reactive message flow with backpressure, identical to that defined in the package org.reactivestreams and/or class java.util.concurrent.Flow.
4. Reversed reactive message flow with backpressure. It is similar to the reactive message stream described above, 
but messages are sent not from Publisher to Subscribers, but from Subscribers to Publisher, which are named Producers and Consumers, respectively. 
This protocol is an asynchronous analog to the input part of the interface of java.util.concurrent.BlockingQueue (method put(T)),
while org.reactivestreams protocol is in fact asynchronous analogue of the output part of that interface. 

Input and output ports can be connected directly, or via connectors - special nodes which provide temporary memory for tokens.
The most significant connector is AsyncArrayBlockingQueue. It implements both asynchronous and synchronous access.
Synchronous access is a subset of the interface java.util.concurrent.BlockingQueue.
It provide bufferization of messages and interoperability with threads. 
 
## The main results of this work

1. Differentiation between nodes and ports. Nodes define behaviour, ports define communications.
This allows to develop ports independently of nodes and make use of new protocols with already developed node types.
As a result, this library is very compact. 
It does not contain fluent API and does not try to implement [all combinations of all capabilities](https://www.google.ru/search?q="all+combinations+of+all+capabilities),
but allow developers to freely combine existing and newly developed capabilities.

2. The type hierarchy of the active nodes is based on two fundamental classes: 
- AsyncProc for single-shot computations
- (dataflow) Actor for recurrent computations

3. AsyncProc, being an asynchronous procedure, does not return value (or better say, returns void value).
Its simple extension AsyncFunc<T> returns a value of arbitrary reference type T.

4. Actor is provided with machinery to implement finite state machine which can be defined by a flow chart. 
This allows to transform parallel algorithm into asynchronous mechanically, preserving the semantics.
See the test DiningPhilosophers as an example of such transformation.

5. Hewitt's Actor (e.g. [Akka](https://akka.io/)) is no more than dataflow Actor with single input message flow parameter.

6. Each communication protocol can be implemented in both synchronous and asynchronous forms. 
Especially useful are nodes which support both synchronous and asynchronous versions.
Thus, the class org.df4j.core.communicator.AsyncSemaphore extends java.util.concurrent.Semaphore with implementation of SignalFlow.Publisher.
Class AsyncArrayBlockingQueue partially implements java.util.concurrent.BlockingQueue, ReverseFlow.Publisher, and Flow.Publisher. 
Such communicators can help when transforming multithreading program to asynchronous form.

7. Reactive streams are just implementation of a specific protocol, and that protocol is no more but a combination of two more simple protocols. 
Reactive streams in asynchronous programming plays the same role as blocking queues in multithreading programming: probably most useful,
but by far not the only way to connect independent parties. 

See examples and test directories for various custom-made dataflow objects and their usage.

If you find a bug or have a proposal, create an issue at <https://github.com/akaigoro/df4j/issues/new>,
or send email to alexei.kaigorodov(at)gmail.com.

## Library structure:

[df4j-protocols](/df4j-protocols/README.md) - Communication interfaces for df4j.

[df4j-core](/df4j-core/README.md) - various predefined types of asynchronous nodes and ports

[df4j-nio2](/df4j-nio2/README.md) - wrappers for NIO2 classes, compatible with df4j interfaces

[df4j-reactivestreams](/df4j-reactivestreams) - runs df4j implementation against reactive streams tests (<https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck>)

Version history
---------------
2020/02/05
version 8.2.
BasicBlock eliminated. AsyncProc became the root async node. 
Methods awake() and awake(long delay) eliminated. 
Functionality of BasicBlock is modelled with Actor and new stop() method.
Old stop methods renamed to onComplete and onError.
Actor has new methods nextAction(ThrowingRunnable), suspend(), and delay(millis), to model a control flow chart.

2019/12/30
version 8.0.
All the tests passed, including those from reactive TCK. 
The branch API-8 becomes the default branch of the https://github.com/akaigoro/df4j project.

2019/08/26 
Branch API-8 started: protocols refactored; total simplification. 
ReverseFlow and AsyncBlockingQueue introduced.

2019/06/16
tag 7.2 protocol interfaces are grouped in a separate module df4j-protocols.

2019/05/04
Branch API-7 and tag ver7.0 created.
tag ver7.1: scalars made compatible with RxJava2

2018/07/15
Branch API-5 and tag ver5.0.1 created.

2018/07/10
Tag ver4.2 in branch API-4 created.

2018/05/16
Tag ver4.1 in branch API-4 created.

2018/05/06
Branch API-4 and tag ver4.0 created.

2017/10/20
pom.xml files corrected. Branch API-3 and tag ver3.1 created.

2015/06/21
Converted to multimodule maven project. Previuos df4j project is now module df4j-core.

2015/06/20
Further codebase and interface minimization, version number 3.0

20/06/15
v1 branch development freesed, tagged as  df4j-core-v1.0
v2 branch development freesed, tagged as  df4j-core-v2.0

2014/04/06
Refactored for more clean design and structure. Interface cahnged, version number 2.0  

2013/09/01
df4j-core proved to be stable, so version number 1.0 is assigned.  
