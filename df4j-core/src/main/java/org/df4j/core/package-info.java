/**
 * Provides the core classes for dataflow and actor programming.
 * Dataflow programming model has many flavors. 
 * This implementation has following features:
 * <p>
 * - computational dataflow graph is represented as tree of nodes
 * <p>
 * - nodes are created dynamically
 * <p>
 * - nodes have ports to accept and send tokens (messages and signals).
 * <p>
 * - the framework activates node after all the ports are unblocked:
 *  - input ports carry incoming tokens
 *  - output ports has memory to save output messages.
 *  There are no output ports for signals, as they do not require memory.
 * <p>
 * - nodes are executed by an executor set at the period of time between node creation and first execution.
 * If no executor is set, then {@link java.util.concurrent.ForkJoinPool} is used.
 * <p>
 * - nodes are subclasses of abstract class {@link org.df4j.core.actor.BasicBlock}.
 * <p>
 * - ports may be of several predefined types, and user can create specific pin types
 * by subclassing class {@link org.df4j.core.actor.BasicBlock.Port}.
 *
 */

package org.df4j.core;