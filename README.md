Simplicity is prerequisite for reliability. - Edsger W. Dijkstra
-------------------------

df4j is an abbreviation of "Data Flow for Java". It is a library to support asynchronous computations. 
For those interested in history of dataflow programming, I recommend to start with short introductory article
"Dataflow Programming: Concept, Languages and Applications" by Tiago Boldt Sousa.

The primary goal of this library is to investigate the anatomy of asynchronous programming.
The asynchronous programming always attracted Java programmers,
and the absence of a complete asynchronous support in language and runtime only stimulated programmers to find their own solutions.
Today some asynchronous libraries for Java are very popular, e.g. rx-java, vert.x, Akka.
However, all they imply steep learning curve and hides implementation details under the hood.
df4j ia an attempt to discover the basic building elements of asynchronous computations,
and allow developer to freely combine those elements, and add new ones.

The main results of this work are listed below. Some of them look evident, but listed for completeness.

1. Parallel computation can be represented as a (dataflow) graph, which consists of 2 kinds of nodes: activities and connectors.
Activities compute tokens (values and signals), connectors pass them between activities.
2. Activities can be of two kinds: threads and asynchronous procedures.
3. Asynchronous procedure consists of:
  - asynchronous parameters (represented as connectors)
  - user-defined synchronous procedure,
  - reference to an Executor, and
  - an object that glues all that components together. Below this object is referenced as a "node of dataflow graph", or just a "node".
4. Connector has several important characteristics:
 -  it can be bound to a node and serve as a mandatory asyncronous parameter.
 The execution of the node starts exactly after all such parameters are filled with tokens. 
 This mechanism is the base of asynchronoys computations.
 - can be used for input or output.
Asynchronous procedure does not produce return value, as synchronous procedures usually do, so output connectors are necessary. 
A node can have multiple input and multiple output connectors.
Nodes are connected by their connectors: output connector of one node is connected to input connector of another node.
 - Connectors can implement different exchange protocols. Connected connectors must implement the same protocol.
 
The main result is differentiation between connectors and nodes. 
This allows to develop connectors independently of nodes and make use of new protocols with already developed node types.
As a result, this library is very compact. 
It does not contain fluent API and do not tries to implement [all combinations of all capabilities](https://www.google.ru/search?q="all+combinations+of+all+capabilities),
but allow developers to freely combine existing and newly developed capabilities.

Another interesting result is that the notorious reactive streams are just implementation of a specific protocol, and that protocol is no more
but a combination of two more simple protocols. 
Reactive streams in asynchronous programming plays the same role as blocking queues in multithreading programming: probably most useful,
but by far not the only way to connect independent parties. 

See examples and test directories for various custom-made dataflow objects and their usage.

If you find a bug or have a proposal, create an issue at <https://github.com/akaigoro/df4j/issues/new>,
or send email to alexei.kaigorodov(at)gmail.com.

Submodiles:

[df4j-core](/df4j-core/README.md) - various predefined types of asynchronous nodes and connectors

[df4j-nio2](/df4j-nio2/README.md) - wrappers for NIO2 classes, compatible with df4j interfaces

Version history
---------------
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
