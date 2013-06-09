df4j is a basic dataflow library. It can easily be extended for specific needs.

The primary goal is to extend java.util.concurrent package with means to synchronize tasks
task submissions to a thread pool. Tasks are treated as procedures with parameters,
which are calculated asynchronously in other tasks.
When all parameters are set, the task is submitted to the executor, attached to the task when the task was created.
For convenience, default executor can be created which can be attached to the task implicitly.
When a task executes, it calculates and passes parameters to other tasks, which eventually causes their execution.
So the tasks form a directed (but not necessarily acyclic) graph where the tasks are nodes and parameter assignments are arcs.
This graph is named dataflow graph. It also can be considered as a colored Petri net with some restrictions.
Transitions are defined together with input places in an instance of core class DataflowVariable,
so places cannot submit tokens to more than one transition. Also, DataflowVariable contains implicit token loop which
prevents parallel executions of the transition. Multiple executions can occur when
parameters are assigned multiple times.    

Subprojects
-----------

df4j-core: contains core functionality. Requires java 1.6 or higher.

df4j-nio1: a wrapper to nio asynchronous input-output functionality (based on Selector).

df4j-nio2: a wrapper to nio2 asynchronous input-output functionality. Requires java 1.7 or higher.

df4j-nio: common parts of df4j-nio1 and df4j-nio2.

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
        coll.post("Hello");
        coll.post("World");
        coll.post("");
    }
</pre>

That's it. No additional object creation, like Properties and ActorSystem in Akka, or Fiber in JetLang.
Well, that objects can be useful under some circumstances, but why force programmer to use them always?
df4j is built around a few number of simple principles, and as long as programmer follows that principles,
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
        coll.post("Hello");
        coll.post("World");
        coll.close();
    }
</pre>

We started with an Actor example, as actor model is widely known. However, df4j treats actors as a special case of a node in
dataflow graph, namely, a node with one explicit input arc (there is also an implicit arc which
makes a loop and holds one token - the state of the node instance). Below is an implementation based on naked DataflowNode:

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
        coll.input.post("Hello");  // there is no predifined input,
        coll.input.post("World");  // input has to be named explicetly
        coll.input.close();
    }
</pre>

So, an Actor is a DataflowNode which:
- has predefined variable StreamInput input
- has Port interface shorted to that input
- has act() method parameterized with the value extracted from that input

All these features are convenient but do not give any radical improvements.
Moreover, being an Actor does not prevent from adding more inputs, and indeed
many Actors from the tests and examples are in fact DataflowNodes with several
inputs and cannot be represented as JetLang or Akka actors.

Dataflow Programming
--------------------

DataflowNode can contain multiple inputs and so can solve many tasks
which are difficult to solve with classic Actors. 

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
            actor.post(task);
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
            actor.post(task);
        }
    }
</pre>
This is because Actor is a simple extension of DataflowNode with one declared StreamInput.
Actor is convenient to use, as it is itself implements interface Port (shorted to the predefined input),
and we can write actor.post(m) instead of actor.input.post(m).

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
          	dispatcher.post(actor);
        }

		// called by network connection
        Port<Request> handler = new Port<Request>() {
            public void post(Request r) {
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
        	conn.post(task);
        	semaActor.post(this);
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
When a DataflowNode (including Actor) is created, it should be assigned an Executor to run on.
This library allows two ways of assigning Executor to a DataflowNode: explicitly by a constructor
or implicitly by a ThreadLocal variable.

Via constructor, Executor may be null. In this case,
the node will be executed on the caller's thread (which invokes the post method).
This is safe and fast, but implies no parallelism.
It is recommended for nodes which simply redirect incoming messages, like Dispatcher or WorkerActor in the
examples above: add them a constructor with super(null). Equivalently, extend them from 
DataflowVariable or ActorVariable, respectively.

When a no-arg constructor is used, this makes DataflowNode to take Executor from thread context.
If no executor in the thread context found, new default Executor is created (with fixed number of threads
equal to the number of available processors). If another kind of context executor wanted, create it before
instantiating any DataflowNode and set in context by DFContex.setCurrentExecutor().
Take care for executor's threads to have references to that executor.
Class ContextThreadFactory can be used for this purpose - see DFContext.newFixedThreadPool(nThreads)
and other similar methods. See also the package df4j.ext for a number of specific executors. PrivateExecutor
contains a separate thread to serve one actor - this allow that actor to block on monitors or input/outut operations.
ImmediateExecutor, being set as a context executor, has effect of setting null executor for all nodes,
and turns your program into sequential one, which can help in debugging.

Thread context is an instance of class DFContext and is stored as a local variable. Besides it main purpose to store
current executor, it can be used to keep any other values in a fashion similar to Threadlocal.
Define and use static variables of type DFContext.ItemKey just as you used to use Threadlocals.
The difference is that when spawning new Thread, you should only care to pass DFContext,
and all ItemKeys would be passed with it. 
 

Version history
---------------
v0.8 2013/06/10
Core classes CallbackPromise and CallbackFuture combined in single class ListenableFuture.
Core classes Link and DoublyLinkedQueue removed.
The root of dataflow nodes class hierarchy is now DataflowVariable, which has no link to any Executor and so
executes the method fire on caller's stack.
Many minor simplifications, such as implementing 'fired' token as a bit in the bit mask and not as a separate variable.

---------------
v0.7 2013/01/16
- important rename in core classes:
Port.send => Port.post
Callback.sendFailure => Callback.postFailure
Promise -> CallbackPromise
EventSource => Promise

- refactoring of nio* projects:
implementation based on NIO1 moved from df4j-nio to df4j-nio1;
df4j-nio contains now only common parts of df4j-nio1 and df4j-nio2,
so both df4j-nio1 and df4j-nio2 require the df4j-nio project.

=======
v0.6 2012/12/03
nio project basically finished
=======
v0.5.2 2012/11/27
DFContext class created - a collection of all context resources, including current executor.

v0.5.1 2012/11/19
- class MessageQueue renamed to Dispatcher.
- synchronization in DataflowNode made by j.u.c.ReentrantLock.
 
v0.5 2012/11/17
- core classes renamed:
BaseActor => DataflowNode; 
DataSource => EventSource; 
ThreadFactoryTL => ConextThreadFactory; 
LinkedQueue => DoublyLinkedQueue; 
SerialExecutor moved to the package ext.
- Actor input queue is now pluggable.
- DataflowNode has its own run method, which consumes tokens. Now only new act() method should be overriden.
- DataflowNode has new method sendFailure, to create Callbacks easily.
  It as accompanied with back-end method handleException(Throwable).
- New core class MessageQueue created (suggest a better name).
=======
v0.4 2012/07/07 nio2: IO requests are handled by actors (IOHandlers).
Timer class created with interface similar to AsyncChannel. 

v0.3 2012/05/26 Core project simplified and minimized. Nio project deleted.
Tagged dataflow extracted into a separate project (demux). 
Classes partially documented.

v0.2 2011/02/04 the project split in 3: core (universal), nio (for jdk1.6), nio2 (for jdk1.7)

v0.1 2011/09/22 initial release
