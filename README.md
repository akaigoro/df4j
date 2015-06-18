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

See examples and test directories for various custom-made dataflow objects and their usage.

If you find a bug or have a proposal, create an issue at https://github.com/rfqu/df4j/issues/new,
or send email to alexei.kaigorodov($)gmail.com.

Flow based programming approach
-----------------
This library follows the concept of [Flow based programming](https://en.wikipedia.org/wiki/Flow-based_programming).
Visit that site to see pictures of FBP diagrams and grasp the idea.

Terminilogy notes: here FBP diagram is called dataflow graph, and its component process called a dataflow node, or an actor. Note that the term
Actor can also denote a dataflow node with single input, as does Akka library.

So, node of a dataflow graph consists of frontend and backend.
Frontend contains one or more pins (like pins of a microchip), which accept and store input tokens.
When all pins are ready (have received tokens), then backend procedure is called. Backend procedure reads the input tokens,
perform calculations and sends that (or new) tokens to pins of other (or the same) nodes.

This idea can be impelmented in many ways, depending of desgn decisions chosed:

- what kinds of tokens are possible
- how tokens are passed to nodes
- how backend procedure is called
- how tokens are deleted from inputs.

In df4j, following decisions were made:

- tokens are either references to arbitrary java objects, or just signals which cannot be denoted but can be counted.

- tokens are passed to pins directly with a simple method call, usually Port.post(token).
General contract is that this method call should be fast,
and sender need not bother to organise this call as a separtate task.

- the way the backend procedure is executed is determined at the time of creation of the component, by assigning an executor the the component.
df4j provides several kinds of executors (and programmer can define custom executor), but most of them require that backend procedure cannot block
or sleep, avoiding thread starvation.
In case if the algorithm of the backend procrdure needs, say, to read from network, then another component should be declared, with a pin that receives
network packets, and asynchronous I/O operation should be started, which eventually passes the result to that component.
df4j has nonblocking network library, which support both NIO1 and NIO2.

 - tokens are deleted from inputs only after the backend procedure completes. This way backend procedure can read tokens 
directly from inputs during the execution. Invocations of the backend procedure are serialized with respect to the component instance:
even if all inputs have many tokens, they are processed serially, thus the backend procedure need not synchronize on the component body.

As a result, dataflow graph can contain millions of components, serving thousands of network connections, and using a small number of threads. 

API overview
------------

Base class of a graph component is [DataflowNode](df4j-core/src/com/github/rfqu/df4j/core/DataflowNode.java).
To build a component, user have to define pins and backend procedure. Pins are instances of predefined inner classes.
Backend procedure has signature void act(). Most important pin classes are:

- Semafor: a resource counter. Each execution of the backend procedure consumes one unit of the resource.
When all resources are exhausted, the excution would not start.

- StreamPort: a queue of tokens represented as java objects.

Hello world for StreamInput:
------------------------
<pre>
    class Collector extends DataflowNode {
        StreamInput<String> input=new StreamInput<String>();
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

Since Collector has single input, it can be refactored to an Actor.
Note that this simplifies both access to frontend pins and declaration of backend procedure,
but adds nothing essential - just a sintactic sugar.


Single-input Actor Hello World Example
------------------------

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

Very often actor have to do some action when input stream of messages ended. In the above example,
the end of stream is coded as empty string. This is not convenient: the act method have to check each messsage,
and using a "poison pill" value may not be feasible. So the Actor class has frontend method close and
corresponding method complete for this cases: 

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

Note, being an Actor does not preclude a node from adding more inputs, and indeed
many Actors from the tests and examples are in fact DataflowNodes with several
inputs (and cannot be represented as JetLang or Akka actors).

Actors as tokens
--------------------

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
Executors used in df4j need to implement simple java.util.concurrent.Executor interface. 
When a Task (including DataflowNode or Actor) is created, it should be assigned an Executor to run on.
If Executor argument is null (or parameterless constructor used),  Executor is taken from the thread context.
If no executor in the thread context found, new default Executor is created (with fixed number of threads
equal to the number of available processors). If another kind of context executor wanted, create it before
instantiating any DataflowNode and set in context by DFContex.setCurrentExecutor().
Take care for executor's threads to have references to that executor.
Class ContextThreadFactory can be used for this purpose - see DFContext.newFixedThreadPool(nThreads)
and other similar methods. See also the package df4j.ext for a number of specific executors. PrivateExecutor
contains a separate thread to serve one actor - this allow that actor to block on monitors or input/outut operations.
ImmediateExecutor runs tasks on caller's thread and turns your program into sequential one, which helps in debugging.

Thread context is an instance of class DFContext and is stored as a local variable. Besides it main purpose to store
current executor, it can be used to keep any other values in a fashion similar to Threadlocal.
Define and use static variables of type DFContext.ItemKey just as you used to use Threadlocals.
The difference is that when spawning new Thread, you should only care to pass DFContext,
and all ItemKeys would be passed with it. 
 

Version history
---------------
v1.0 2013/09/01
df4j-core proved to be stable, so version number 1.0 is assigned.  
