package org.df4j.core.actor;

import org.df4j.core.connector.Completion;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * A dataflow graph, consisting of 1 or more {@link AsyncProc}s and, probably, nested {@link ActorGroup}s.
 * Completion signals (errors or success) propagate from the leaf nodes to the root node.
 * Component {@link AsyncProc}s plays the same role as basic blocks in a flow chart.
 */
public class ActorGroup extends Node<ActorGroup> {
    protected final Set<Node> children = Collections.newSetFromMap( new IdentityHashMap<>());
    protected long totalChildCount = 0;

    /**
     *  creates root {@link ActorGroup} graph.
     */
    public ActorGroup() {}

    /**
     *  creates nested {@link ActorGroup} graph.
     * @param parent the parent {@link ActorGroup}
     */
    public ActorGroup(ActorGroup parent) {
        super(parent);
    }

    /**
     * indicates that a node has added to this graph.
     * @param node the node which entered the group
     * @return unique sequential number of the child within this dataflow,
     *         starting from 0.
     */
    public long enter(Node node) {
        synchronized(this) {
            long res = totalChildCount++;
            children.add(node);
            return res;
        }
    }

    /**
     * indicates that a node has left this graph because of successful completion.
     * when all the nodes has left this graph, it is considered successfully completed itself
     * and leaves the pareng graph, if any.
     * @param node the node which leaves the group
     */
    public synchronized void leave(Node node) {
        children.remove(node);
        if (children.size() == 0) {
            super.complete();
        }
    }

    public synchronized void leaveExceptionally(Node node, Throwable ex) {
        children.remove(node);
        super.completeExceptionally(ex);
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAlive() {
        return !completion.isCompleted();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!isCompleted()) {
            sb.append("not completed");
        } else if (completion.getCompletionException() == null) {
            sb.append("completed successfully");
        } else {
            sb.append("completed with exception: ");
            sb.append(completion.getCompletionException().toString());
        }
        sb.append("; child node count: "+children.size());
        return sb.toString();
    }
}
