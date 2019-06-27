/**
 * Provides the core classes for dataflow and actor programming.
 * Dataflow programming model has many flavors. 
 * This implementation has following features:
 * <p>
 * - computational dataflow graph is represented as a set of nodes
 * <p>
 * - nodes are created dynamically
 * <p>
 * - nodes have pins to accept tokens (messages).
 * <p>
 * - the framework activates node after all the pins carry incoming tokens
 * <p>
 * - nodes are executed by an executor determined at the time of node creation. 
 * Executor can be set directly as a constructor argument, or taken implicitly 
 * from thread context.  
 * <p>
 * - nodes are subclasses of abstract class {@link org.df4j.core.asyncproc.AsyncProc}.
 * <p>
 * - pins may be of several predefined types, and user can create specific pin types
 * by subclassing class {@link org.df4j.core.asyncproc.base.ScalarLock}.
 *
 *
 * ### The Anatomy of Asynchronous procedure.
 * ------------------------------------------
 * An asynchronous procedure is a kind of parallel activity, along with a Thread.
 * It differs from a thread so that while waiting for input information to be delivered, it does not use procedure stack and so does not wastes core memory.
 * As a result, we can manage millions of asynchronous procedures, while 10000 threads is already a heavy load.
 * This can be important, for example, when constructing a web-server.
 *
 * An asynchronous procedure differs from ordinary synchronous procedure in the following way:
 *  - it resides in the heap and not on a thread's stack
 *  - its input and output parameters are not simple variables but separate objects in the heap
 *  - it starts execution when all its parameters are ready
 *  - it executes not on the caller's thread, but on the Executor, assigned to the procedure at the moment of creation.
 *
 * So: parameter objects are tightly connected to the asynchronous procedure object, and parameters cooperate to detect the moment when all the parameters are ready, and then submit the procedure to the executor.
 *
 * Input parameters are connected to output parameters of other procedures. As a result, the whole program is represented as a (dataflow) graph.
 * Each parameter implements some connecting protocol. Connected parameters must belong to the same protocol.
 * ### The two Realms of Asynchronous Programming:
 * ### Scalar realm {@link org.df4j.core.asyncproc}
 * ### Actor realm {@link org.df4j.core.actor}
 */
package org.df4j.core;