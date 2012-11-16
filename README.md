df4j is a basic dataflow library. It can easily be extended for specific needs.

Subprojects
-----------

df4j-core: contains core functionality. It requires java 1.5 or higher.
df4j-demux (in progress): base for remote and/or persistent actors.
df4j-nio2: a wrapper to nio2 asyncronous input-output functionality. It requires java 1.7 or higher.

See examples and test directories for various custom-made dataflow objects and their usage.

If you find a bug or have a proposal, create an issue at https://github.com/rfqu/df4j/issues/new,
or send email at alexei.kaigorodov($)gmail.com.

Hello World Example
-------------------

<pre>
    class Collector extends Actor<String> {
        StringBuilder sb=new StringBuilder();
        
        @Override
        protected void act(String message) throws Exception {
            if (message.length()==0) {
                out.println(sb.toString());
                System.out.flush();
            } else {
                sb.append(message);
                sb.append(" ");
            }
        }
        
    }

    public void test() throws InterruptedException {
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


Version history
---------------

v0.5 2012/11/17
- core classes renamed:
BaseActor => DataflowNode
DataSource => EventSource
ThreadFactoryTL => ConextThreadFactory
LinkedQueue => DoublyLinkedQueue
SerialExecutor moved to ext.
- Actor input queue is now pluggable.
- DataflowNode has its own run method. Now only new act() method should be overriden.
  Tokens are consumed automatically when the node is fired.
- DataflowNode has new method sendFailure, to create Callbacks easily.
  It as acompanied with back-end method handleException(Throwable).
- New ext class Demux created.

v0.4 2012/07/07 nio2: IO requests are handled by actors (IOHandlers).
Timer class created with interface similar to AsyncChannel. 

v0.3 2012/05/26 Core project simplified and minimized. Nio project deleted.
Tagged dataflow extracted into a separate project (demux). 
Classes partially documented.

v0.2 2011/02/04 the project split in 3: core (universal), nio (for jdk1.6), nio2 (for jdk1.7)

v0.1 2011/09/22 initial release
