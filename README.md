df4j is a basic dataflow library. It can easily be extended for specific needs.

Subprojects
-----------

df4j-core: contains core functionality. It requires java 1.5 or higher.

df4j-nio (in progress): a wrapper to nio asyncronous input-output functionality (Baset on Selector).

df4j-nio2: a wrapper to nio2 asyncronous input-output functionality. It requires java 1.7 or higher.

See examples and test directories for various custom-made dataflow objects and their usage.

If you find a bug or have a proposal, create an issue at https://github.com/rfqu/df4j/issues/new,
or send email to alexei.kaigorodov($)gmail.com.

Hello World Example
-------------------

<pre>
    class Collector extends Actor<String> {
        StringBuilder sb=new StringBuilder();
        
        @Override
        protected void act(String message) {
            if (message.length()==0) {
                System.out.println(sb.toString());
            } else {
                sb.append(message);
                sb.append(" ");
            }
        }
    }

    public void test() {
        Collector coll=new Collector();
        coll.send("Hello");
        coll.send("World");
        coll.send("");
    }
</pre>

That's it. No additional object creation, like Properties and ActorSystem in Akka, or Fiber in JetLang.
Well, that objects can be useful under some circumstances, but why force programmer to use them always?
df4j is build around a few number of simple principles, and as long as programmer follows that principles,
he can extend the library in any direction.

Very often actor have to do some action when input stream of messages ended. In the above example,
the end of stream is coded as empty string. This is not convenient: the act method have to check each messsage,
and using a "poison pill" value may not be feasible. So the Actor class has methods close and complete for this cases: 

<pre>
    class Collector extends Actor<String> {
        StringBuilder sb=new StringBuilder();
        
        @Override
        protected void act(String message) {
            sb.append(message);
            sb.append(" ");
        }
        @Override
        protected void complete(){
            System.out.println(sb.toString());
        }
    }

    public void test() {
        Collector coll=new Collector();
        coll.send("Hello");
        coll.send("World");
        coll.close();
    }
</pre>

We started with Actor example, as actor model is widely known. However, df4j treats actors as a special case of a node in
dataflow graph, namely, a node with one explicit input arc (there is also an implicit arc which
makes a loop and holds one token - the state of the node instance). Below is an implementation based on DataflowNode:

<pre>
    class Collector extends DataflowNode {
        Input<String> input=new StreamInput<String>();
        StringBuilder sb=new StringBuilder();
        
        @Override
        // since dataflow node can have different number of inputs,
        // the act method have no parameters, values from inputs
        // has to be extracted manually.
        protected void act() {
            String message=input.get();
            if (message==null) {
                // StreamInput does not accept null values,
                // and null value signals that input is closed
                System.out.println(sb.toString());
            } else {
               sb.append(message);
               sb.append(" ");
            }
        }
    }

    public void test() {
        Collector coll=new Collector();
        coll.input.send("Hello");
        coll.input.send("World");
        coll.input.close();
    }
</pre>

Dataflow Programming
--------------------

DataflowNode can contain multiple inputs and so can solve many tasks
which are difficult to solve with Actors. 

Let we have several worker actors, for example, representing computational nodes in cluster.
The actors accept messages with assignments. Cluster users would like to have a single port
to send assignments, and the reactor of that port dispatches messages to worker actors.
Simple and elegant dataflow solution is as follows: dispatcher has 2 port: one for assignments
and one for worker actors. Each actor monitors its node and when it knows that the node is able
to receive new assignments, the actor sends itself to the actor's port. Actors are just objects and can 
be sent as messages (of course, references to actors). The dispatcher acts when both input ports
are not empty:
<pre>
    class Dispatcher extends DataflowNode {
        Input<Assignment> tasks=new StreamInput<Assignment>();
        Input<Actor<Assignment>> actors=new StreamInput<<Actor<Assignment>>>();
        
        @Override
        protected void act() {
            Assignment task=tasks.get();
            Actor<Assignment> actor=actor.get();
            actor.send(task);
        }
    }
</pre>
In fact, Dispatcher can be build upon Actor:
<pre>
    class Dispatcher extends Actor<Assignment> {
        Input<Actor<Assignment>> actors=new StreamInput<<Actor<Assignment>>>();
        
        @Override
        protected void act(Assignment task) {
        	Actor<Assignment> actor=actors.get();
            actor.send(task);
        }
    }
</pre>
This is because Actor is a simple extension of DataflowNode with one declared StreamInput.
Actor is convenient to use, as it is itself implements interface Port (shorted to the predefined input),
and we can write actor.send(m) instead of actor.input.send(m).

Now think how worker actor could know if its node can be given more assignments.
Suppose we just decide that when connecting to the cluster node, it replies with a number of tasts
it can execute simultaneously. Sending an assignment reduces this number by one. When the node
finishes one task, it sends a message and the number is increased by one.

In multithreaded environment, such a resource counter can be represented with a semaphore.
In actor environment, actors are not allowed to block on semaphores. Instead, they can use 
equivalent feature: class DataflowNode.Semafor. It reminds DataflowNode.Input, but does not holds
messages, but only a counter, allowing the dataflow node to execute only when the counter is greater than zero.
Each execution of the DataflowNode.act() method reduces the counter by one.

To imitate acquiring semaphore, we create an intermediate Actor with Semafor instantiated. Working actor
sends itself to that intermediate actor and, if semaphore is open, working actor is sent further to the dispatcher. 

<pre>
    class SemaActor extends Actor<Actor<Assignment>> {
    	Semafor counter=new Semafor();
    	Dispatcher dispatcher; // initialize by IOC or in constructor
        
        @Override
        protected void act(Actor<Assignment> actor) {
          	dispatcher.send(actor);
        }

		// called by network connection
        Port<Request> handler = new Port<Request>() {
            public void send(Request r) {
            	counter.up();
            }
        }
    }

    class WorkerActor extends Actor<Assignment> {
    	SemaActor semaActor=new SemaActor();
        NetworkConnection conn=new NetworkConnection(clusterNodeAddress, semaActor);
        
        @Override
        protected void act(Assignment task) {
            // create new request for the network connection
        	Request r=new Request(task, this);
            conn.send(task);
           	semaActor.send(this);
        }        
    }
</pre>   
In the above example, network connection only resends outgoing messages without any confirmation, and receives
incoming messages of one type (permission to send one more assignment to the cluster node).
In reality, network communication is much more complicated.

DataflowNode with an Input and Semafor (or an Actor with Semafor inside) 
is a powerful facility to represent nested non-blocking services. See program NestedCallbacks in the tutorial package.  

Background Executor
-------------------
When a DataflowNode (including Actor) is created, it must be assigned an Executor to run on.
This library allows two ways of assigning Executor to a DataflowNode: explicitly by a constructor
or implicitly by a ThreadLocal variable.

Via constructor, Executor may be null. In this case,
the node will be executed on the caller's thread (which invokes the send method). This is safe and fast, but implies no parallelism.
It is recommended for nodes which simply redirect incoming messages, like Dispatcher or WorkerActor in the
examples above: add them a constructor with super(null). Equivalently, extend them from 
DataflowVariable or ActorVariable, respectively.

When a no-arg constructor is used, this makes DataflowNode to take Executor from thread context.
If no executor in the thread context found, new Executor created with fixed number of threads
equal to the number of available processors. If another kind of context executor wanted, create it before
instantiating any DataflowNode and set in context by DFContex.setCurrentExecutor().
Take care for executor's threads to have references to that executor.
Class ContextThreadFactory can be used for this purpose - see DFContext6.ContextThreadFactory.newFixedThreadPool(nThreads)
and other similar methods. See also the package df4j.ext for a number of specific executors. PrivateExecutor
contains a separate thread to serve one actor - this allow that actor to block on monitors or input/outut operations.
ImmediateExecutor, being set as a context executor, has effect of setting null executor for all nodes,
and turns your program into sequential one, which can help in debugging.

Thread context is an instance of class DFContext and is stored as a local variable. Pecularity of
this class is that it is present both in df4j-core and df4j-nio2 projects in different version. This is because df4j-nio2
requires context extended with java7 features, and including this context in df4j-core would break compatibility with java6.
So when using df4j-nio2, be careful to keep its classes before df4j-core classes in class path.
 

Version history
---------------

v0.5.1 2012/11/17
- class MessageQueue renamed to Dispatcher.
- synchronization in DataflowNode made by j.u.c.ReentrantLock.
 
v0.5 2012/11/17
- core classes renamed:
BaseActor => DataflowNode
DataSource => EventSource
ThreadFactoryTL => ConextThreadFactory
LinkedQueue => DoublyLinkedQueue
SerialExecutor moved to the package com.github.rfqu.df4j.ext.
=======
v0.5 2012/11/19
- core classes renamed:
BaseActor => DataflowNode; 
DataSource => EventSource; 
ThreadFactoryTL => ConextThreadFactory; 
LinkedQueue => DoublyLinkedQueue; 
SerialExecutor moved to ext.
- Actor input queue is now pluggable.
- DataflowNode has its own run method, which consumes tokens. Now only new act() method should be overriden.
- DataflowNode has new method sendFailure, to create Callbacks easily.
  It as accompanied with back-end method handleException(Throwable).
- New core class MessageQueue created (suggest a better name).

v0.4 2012/07/07 nio2: IO requests are handled by actors (IOHandlers).
Timer class created with interface similar to AsyncChannel. 

v0.3 2012/05/26 Core project simplified and minimized. Nio project deleted.
Tagged dataflow extracted into a separate project (demux). 
Classes partially documented.

v0.2 2011/02/04 the project split in 3: core (universal), nio (for jdk1.6), nio2 (for jdk1.7)

v0.1 2011/09/22 initial release
