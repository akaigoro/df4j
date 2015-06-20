decide:
1 port is closed, ждать ли заполнениея остальных, чтобы запустить onClose?

-------------------------
df4j is a compact dataflow actor library. It can easily be extended for specific needs.

The primary goal is to extend java.util.concurrent package with means to synchronize
task submissions to a thread pool. Tasks can be treated as procedures with parameters,
values of that parameters are calculated asynchronously and concurrently in other tasks.
The library provides all the nessessary synchronization, so programmer need not use the synchronized operator or ReentranLocks. 
When all parameters are computed, the task is submitted to the executor.
When a task executes, it calculates and passes actual parameters to other tasks, which eventually causes their execution.
After task finishes, it can run again after new set of parameters is supplied.
So the tasks form a directed and, probably, cyclic graph where the tasks are nodes and parameter assignments are arcs.
This graph is named dataflow graph. 

See examples and test directories for various custom-made dataflow objects and their usage.

If you find a bug or have a proposal, create an issue at https://github.com/rfqu/df4j/issues/new,
or send email to alexei.kaigorodov(at)gmail.com.







Version history
---------------
v1.0 2013/09/01
df4j-core proved to be stable, so version number 1.0 is assigned.  
