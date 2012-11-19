df4j is a basic dataflow library. It can easily be extended for specific needs.

Subprojects
-----------

df4j-core: contains core functionality. It requires java 1.5 or higher.

df4j-demux (in progress): base for remote and/or persistent actors.

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
        Input<String> input=new StreamInput<String>(new ArrayDeque<String>());
        StringBuilder sb=new StringBuilder();
        
        @Override
        // since dataflow node can have different number of inputs,
        // the act method have no parameters, values from inputs
        // has to be extracted manually.
        protected void act() {
            String message=input.value;
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



Version history
---------------

v0.5 2012/11/19
- core classes renamed:
BaseActor => DataflowNode; 
DataSource => EventSource; 
ThreadFactoryTL => ConextThreadFactory; 
LinkedQueue => DoublyLinkedQueue; 
SerialExecutor moved to ext.
- Actor input queue is now pluggable.
- DataflowNode has its own run method. Now only new act() method should be overriden.
  Tokens are consumed automatically when the node is fired.
- DataflowNode has new method sendFailure, to create Callbacks easily.
  It as acompanied with back-end method handleException(Throwable).
- New core class MessageQueue created (suggest a better name).

v0.4 2012/07/07 nio2: IO requests are handled by actors (IOHandlers).
Timer class created with interface similar to AsyncChannel. 

v0.3 2012/05/26 Core project simplified and minimized. Nio project deleted.
Tagged dataflow extracted into a separate project (demux). 
Classes partially documented.

v0.2 2011/02/04 the project split in 3: core (universal), nio (for jdk1.6), nio2 (for jdk1.7)

v0.1 2011/09/22 initial release
