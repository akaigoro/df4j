/**
 * Main components of the dataflow graph: {@link org.df4j.core.actor.Node} and {@link org.df4j.core.actor.ActorGroup},
 * {@link org.df4j.core.actor.AsyncProc} and {@link org.df4j.core.actor.Actor}.
 *
 * Node
 * |
 * ActorGroup  AsyncProc       Actor        AsyncFunc    ActorFunc
 * completion completion     completion       Scalar       Scalar
 *     -     TransitionAll   TransitionSome  TransitionAll TransitionSome
 *
 */
package org.df4j.core.actor;
