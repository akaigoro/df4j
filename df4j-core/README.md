Everything should be made as simple as possible, but not simpler. - Albert Einstein
------------------- 

How to implement an asynchronous procedure.
------------------------------------------
An asynchronous procedure differs from a thread that while waiting for input information to be delivered, 
it does not use procedure stack and so does not wastes core memory. 
As a result, we can manage millions of asynchronous procedures,
while 10000 threads is already a heavy load. This can be important, for example, when constructing a web-server.

To build an asynchronous procedure, first we need is to move parameters out of procedure stack to the heap.
Second, we need to build an object which calls requred procedure as soon as all the arguments are received.

That's it.

Create an object which knows which ordinary procedure to call, and bound asynchronous paramerters to it.

Sounds simple, but in practice most async libraries tries to oversymplify and do not allow to create parameters separately from
the async procedure. The result is overcomplicated API, which simultanousely is limited in expressiveness.

First, represent your design as a dataflow graph, with nodes representing computations, and arcs representing data transitions. 
The nodes of that graph will be called here "activities". 
Activities can be of two kinds: threads and asynchronous procedures, otherwise named actors.
Threads are more capable, but require much more core memory.
The hard part ofparallel programming is communications between activities, so creating a parallel program is mainly creating communications.
The nature of activities (threads or actors) is not that important.

Communication between synchronous (ordinary) procedures is done by mere writing and reading parameters and return values, mapped on memory cells.
Communication between parallel activities is complicated because reader activity can be ready to accept value which writer activity has not yet computed.
In this cases, threads block until value is available. 
Actors cannot block, as blocking switches off the thread, occupied by the actor, out of service, and the advantage of actors as consuming little memory is lost.
Instead, every point of the algorithm where new data are awaited, must be shaped as a separate actor.  
