/**
 * Main components of the dataflow graph: {@link Dataflow} and {@link BasicBlock},
 * and most used derivatives : {@link org.df4j.core.dataflow.AsyncProc} and {@link org.df4j.core.dataflow.Actor}.
 * <p>
 *  {@link Dataflow} encloses  {@link BasicBlock}s and nested {@link Dataflow}s.
 *  {@link org.df4j.core.dataflow.AsyncProc} is a {@link BasicBlock} within its own  {@link Dataflow} graph,
 *  which executes only once.
 *  {@link org.df4j.core.dataflow.Actor} is a {@link BasicBlock} within its own  {@link Dataflow} graph,
 *  which executes repeatedly.
 *
 */
package org.df4j.core.dataflow;
