
How to write parallel programs
----------------------------------
First, represent your design as a dataflow graph, with nodes representing computations, and arcs representing data transitions. 
The nodes of that graph will be called here "activities". 
Activities can be of two kinds: threads and asynchronous procedures, otherwise named actors.
Threads are more capable, but require much more core memory.
The hard part ofparallel programming is communications between activities, so creating a parallel program is mainly creating communications.
The nature of activities (threads or actors) is not that important.

Communication between synchronous (ordinary) procedures is donr=e by mere writing and reading parameters and return values, mapped on memory cells.
Communication between parallel activities is complicated because reader activity can be ready to accept value which writer activity has not yet computed.
In this cases, threads block until value is available. 
Actors cannot block, as blocking switches off the thread, occupied by the actor, out of service, and the advantage of actors as consuming little memory is lost.
Instead, every point of the algorithm where new data are awaited, must be shaped as a separate actor.  
