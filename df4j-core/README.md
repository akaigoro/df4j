df4j is a basic dataflow library. It can easily be extended for specific needs.

The primary goal is to extend java.util.concurrent package with means to synchronize 
task submissions to a thread pool. Tasks are treated as procedures with parameters,
which are calculated asynchronously in other tasks.
When all parameters are set, the task is submitted to the executor, attached to the task when the task was created.
For convenience, default executor can be created which can be attached to the task implicitly.
When a task executes, it calculates and passes parameters to other tasks, which eventually causes their execution.
So the tasks form a directed (but not necessarily acyclic) graph where the tasks are nodes and parameter assignments are arcs.
This graph is named dataflow graph. It also can be considered as a colored Petri net with some restrictions.
Just like a Petri net,  dataflow graph is bipartite: it consists of places (where data tokens are stored until they are consumed),
and transitions - tasks which take input tokens and issue output tokens to their output places.
  
This library support two kinds of places and transitions: for single-time execution and for stream execution.


Single-time execution package
--------------------------------
Classes and interfaces for single-time execution are collected in org.df4j.core.func package. Main classes are:

 - Promise: a storage for one data token. Token is put explicitly with method post. It then can be distributed to many consumers.
Consumers can request the data synchronously, via interface java.util.concurrent.Future, or asynchronously via subscriptions.
Subscribers must implement core interface Listener.

 - Function: it is a transition together with input places for arguments and output place for result. Input places can be connected
 to Promises or output places of other Functions via subscriptions.

Streame execution package
--------------------------------
Classes and interfaces for single-time execution are collected in org.df4j.core.actor package. Main classes are:

- Actor:  it is a transition together with input places for input streams. Since input places are defined together with transitions,
they can submit tokens to only one transition. Also, Actor contains implicit control token loop which
prevents parallel executions of the transition.

- Actor1: syntactic sugar for Actor with one predefined input place. This kind of actors are similar to actors in Acca andGPars libraries.

- MultiPortActor: handles one message at a time just like Actor1, but with multiple inputs,
each input supplied with its own handler. This can be used to model pattern matching in Akka actors,
and for other purposes.       

See examples and test directories for various custom-made dataflow objects and their usage.

If you find a bug or have a proposal, create an issue at https://github.com/rfqu/df4j2/issues/new,
or send email to alexei.kaigorodov($)gmail.com.

Actor tutorial
------------

Base class of a graph component is [Actor](src/org/df4j/core/actor/Actor.java).
To build a component, user have to define pins and backend procedure. Pins are instances of predefined inner classes.
Backend procedure has signature void act(). Most important pin classes are:

- Semafor: a resource counter. Each execution of the backend procedure consumes one unit of the resource.
When all resources are exhausted, the excution would not start.

- StreamPort: a queue of tokens represented as java objects.

Hello world for StreamInput:
------------------------
<pre>
    class Collector extends Actor {
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

Since Collector has single input, it can be refactored to an Actor1.
Note that this simplifies both access to frontend pins and declaration of backend procedure,
but adds nothing essential - just a sintactic sugar.


Single-input Actor1 Hello World Example
------------------------

<pre>
    class Collector extends Actor1<String> {
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
and using a "poison pill" value may not be feasible. So the StreamInput interface (and so Actor1 class)
has frontend method close and corresponding method complete for this cases: 

<pre>
    class Collector extends Actor1<String> {
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

Note, being an Actor1 does not preclude a node from adding more inputs, and indeed
many Actors from the tests and examples are in fact Actors with several
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
    class Dispatcher extends Actor {
        Input<Assignment> tasks=new StreamInput<Assignment>();
        Input<Actor1<Assignment>> actors=new StreamInput<<Actor1<Assignment>>>();
        
        @Override
        protected void act() {
            Assignment task=tasks.get();
            Actor1<Assignment> actor=actor.get();
            actor.post(task);
        }
    }
</pre>
In fact, Dispatcher can be build upon Actor1:
<pre>
    class Dispatcher extends Actor1<Assignment> {
        Input<Actor1<Assignment>> actors=new StreamInput<<Actor1<Assignment>>>();
        
        @Override
        protected void act(Assignment task) {
        	Actor1<Assignment> actor=actors.get();
            actor.post(task);
        }
    }
</pre>
This is because Actor1 is a simple extension of Actor with one declared StreamInput.
Actor1 is convenient to use, as it is itself implements interface StreamPort (shorted to the predefined input),
and we can write actor.post(m) instead of actor.input.post(m).

Now think how worker actor could know if its node can be given more assignments.
Suppose we just decide that when connecting to the cluster node, it replies with a number of tasts
it can execute simultaneously. Sending an assignment reduces this number by one. When the node
finishes one task, it sends a message and the number is increased by one.

In multithreaded environment, such a resource counter can be represented with a semaphore.
In actor environment, actors are not allowed to block on semaphores. Instead, they can use 
equivalent feature: class Actor.Semafor. It reminds Actor.Input, but does not holds
messages, but only a counter, allowing the dataflow node to execute only when the counter is greater than zero.
Each execution of the Actor.act() method reduces the counter by one.

To imitate acquiring semaphore, we create an intermediate Actor1 with Semafor instantiated. Working actor
sends itself to that intermediate actor and, if semaphore is open, working actor is sent further to the dispatcher. 

<pre>
    class SemaActor extends Actor1<Actor1<Assignment>> {
    	Semafor counter=new Semafor();
    	Dispatcher dispatcher; // initialize by IOC or in constructor
        
        @Override
        protected void act(Actor1<Assignment> actor) {
          	dispatcher.post(actor);
        }

		// called by network connection
        Port<Request> handler = new Port<Request>() {
            public void post(Request r) {
            	counter.up();
            }
        }
    }

    class WorkerActor extends Actor1<Assignment> {
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

Actor with an Input and Semafor (or an Actor1 with Semafor inside) 
is a powerful facility to represent nested non-blocking services. See program NestedCallbacks in the tutorial package.  

Background Executor
-------------------
Executors used in df4j need to implement simple java.util.concurrent.Executor interface. 
When a Task (including Actor or Actor1) is created, it should be assigned an Executor to run on.
If Executor argument is null (or parameterless constructor used),  Executor is taken from the thread context.
If no executor in the thread context found, new default Executor is created (with fixed number of threads
equal to the number of available processors). If another kind of context executor wanted, create it before
instantiating any Actor and set in context by DFContex.setCurrentExecutor().
Take care for executor's threads to have references to that executor.
Class ContextThreadFactory can be used for this purpose - see DFContext.newFixedThreadPool(nThreads)
and other similar methods. See also the package df4j.ext for a number of specific executors. PrivateExecutor
contains a separate thread to serve one actor - this allow that actor to block on monitors or input/outut operations.
ImmediateExecutor runs tasks on caller's thread and turns your program into sequential one, which helps in debugging.

Thread context is an instance of class DFContext and is stored as a tread-local variable. Besides it main purpose to store
current executor, it can be used to keep any other values in a fashion similar to Threadlocal.
Define and use static variables of type DFContext.ItemKey just as you used to use Threadlocals.
The difference is that when spawning new Thread, you should only care to pass DFContext,
and all ItemKeys would be passed with it. 
 

Version history
---------------
v1.0 2013/09/01
df4j-core proved to be stable, so version number 1.0 is assigned.  
v2.0 2014/04/06
Refactored for more clean design and structure.  
