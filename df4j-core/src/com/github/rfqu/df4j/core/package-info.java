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
 * - nodes are subclasses of abstract class {@link com.github.rfqu.df4j.core.BaseActor}.
 *  User have to override  method to retrieve tokens from pins 
 *  {@link com.github.rfqu.df4j.core.BaseActor#consumeTokens()} and method to
 *  handle tokens {@link com.github.rfqu.df4j.core.BaseActor#act()}
 * <p>
 * - pins may be of several predefined types, and user can create specific pin types
 * by subclassing class {@link com.github.rfqu.df4j.core.BaseActor.Pin}
 * <p>
 * - Unlike many other actor libraries, df4j treats actors not as the primary
 * construction blocks, but as a specific type of dataflow node, with a single pin
 * which can store queue of tokens.
 */
package com.github.rfqu.df4j.core;