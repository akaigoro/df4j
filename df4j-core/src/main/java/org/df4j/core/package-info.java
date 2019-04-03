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
 * - nodes are subclasses of abstract class {@link org.df4j.core.asynchproc.AsyncProc}.
 * <p>
 * - pins may be of several predefined types, and user can create specific pin types
 * by subclassing class {@link org.df4j.core.asynchproc.AsyncProc.AsyncParam}
 */
package org.df4j.core;