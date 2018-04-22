/**
 * Provides the core classes for dataflow and actor programming.
 * Dataflow programming model has many flavors. 
 * This implementation has following features:
 * <p>
 * - computational dataflow graph is represented as a set of nodes
 * <p>
 * - nodes are created dynamically
 * <p>
 * - nodes have transition to accept tokens (messages).
 * <p>
 * - the framework activates node after all the transition carry incoming tokens
 * <p>
 * - nodes are executed by an executor determined at the time of node creation. 
 * Executor can be set directly as a constructor argument, or taken implicitly 
 * from thread context.  
 * <p>
 * - nodes are subclasses of abstract class {@link org.df4j.core.impl.Actor}.
 *  User have to override  method to handle tokens
 *  {@link org.df4j.core.impl.Actor#act()}
 * <p>
 * - transition may be of several predefined types, and user can create specific pin types
 * by subclassing class {@link org.df4j.core.impl.Transition.Pin}
 */
package org.df4j.core.impl;