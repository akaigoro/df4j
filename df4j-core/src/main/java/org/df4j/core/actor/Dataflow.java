package org.df4j.core.actor;

import org.df4j.core.util.linked.LinkedQueue;

/**
 * A dataflow graph, consisting of 1 or more {@link AsyncProc}s and, probably, nested {@link Dataflow}s.
 * Completion signals (errors or success) propagate from the leaf nodes to the root node.
 * Component {@link AsyncProc}s plays the same role as basic blocks in a flow chart.
 */
public class Dataflow extends Node<Dataflow> {
    protected LinkedQueue<Node.NodeLink> children = new LinkedQueue<>();
    protected long totalChildCount = 0;

    /**
     *  creates root {@link Dataflow} graph.
     */
    public Dataflow() {
    }

    /**
     *  creates nested {@link Dataflow} graph.
     * @param parent the parent {@link Dataflow}
     */
    public Dataflow(Dataflow parent) {
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
            children.add(node.nodeLink);
            return res;
        }
    }

    /**
     * indicates that a node has left this graph because of successful completion.
     * when all the nodes has left this graph, it is considered successfully completed itself
     * and leaves the pareng graph, if any.
     * @param node the node which leaves the group
     */
    public void leave(Node node) {
        synchronized(this) {
            children.remove(node.nodeLink);
            if (children.size() == 0) {
                super.complete();
            }
        }
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAlive() {
        return !super.isCompleted();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!isCompleted()) {
            sb.append("not completed");
        } else if (this.completionException == null) {
            sb.append("completed successfully");
        } else {
            sb.append("completed with exception: ");
            sb.append(this.completionException.toString());
        }
        sb.append("; child node count: "+children.size());
        return sb.toString();
    }
}
