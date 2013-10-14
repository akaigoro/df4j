df4j is a basic dataflow library. It can easily be extended for specific needs.

The primary goal is to extend java.util.concurrent package with means to synchronize tasks
task submissions to a thread pool. Tasks are treated as procedures with parameters,
which are calculated asynchronously in other tasks.
When all parameters are set, the task is submitted to the executor, attached to the task when the task was created.
Tasks can be reused and run many times, as long as new arguments are submitted.

See [df4j-core/README](df4j-core/README.md) for more details.

Version history
---------------
v1.0 2013/09/01
df4j-core proved to be stable, so version number 1.0 is assigned.  
nio* projects removed. Use [pipeline-nio](https://github.com/rfqu/pipeline/tree/master/pipeline-nio).

---------------
v0.9 2013/07/10
ListenableFuture become an interface, implementation is CompletableFuture.
Request has moved from package ext to package core, and implements Future<Result> and Promise<Request>.
df4j-nio1 deeply refactored, to demonstrate dataflow-style implementation.

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

---------------
v0.6 2012/12/03
nio project basically finished

---------------
v0.5.2 2012/11/27
DFContext class created - a collection of all context resources, including current executor.

---------------
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

---------------
v0.4 2012/07/07 nio2: IO requests are handled by actors (IOHandlers).
Timer class created with interface similar to AsyncChannel. 

v0.3 2012/05/26 Core project simplified and minimized. Nio project deleted.
Tagged dataflow extracted into a separate project (demux). 
Classes partially documented.

v0.2 2011/02/04 the project split in 3: core (universal), nio (for jdk1.6), nio2 (for jdk1.7)

v0.1 2011/09/22 initial release
