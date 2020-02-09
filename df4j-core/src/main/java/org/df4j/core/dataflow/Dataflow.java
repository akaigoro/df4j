package org.df4j.core.dataflow;

import org.df4j.core.util.linked.LinkedQueue;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;

/**
 * A dataflow graph, consisting of 1 or more {@link AsyncProc}s and, probably, nested {@link Dataflow}s.
 * Completion signals (errors or success) propagate from the leaf nodes to the root node.
 * Component {@link AsyncProc}s plays the same role as basic blocks in a flow chart.
 */
public class Dataflow extends Node<Dataflow> implements Activity {
    protected LinkedQueue<Node> children = new LinkedQueue<>();

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

    @Override
    public Dataflow getItem() {
        return this;
    }

    /**
     * indicates that a node has added to this graph.
     * @param node
     */
    public void enter(Node node) {
        bblock.lock();
        try {
            children.add(node);
        } finally {
            bblock.unlock();
        }
    }

    /**
     * indicates that a node has left this graph because of successful completion.
     * when all the nodes has left this graph, it is considered successfully completed itself
     * and leaves the pareng graph, if any.
     * @param node
     */
    public void leave(Node node) {
        bblock.lock();
        try {
            children.remove(node);
            if (children.size() == 0) {
                super.onComplete();
            }
        } finally {
            bblock.unlock();
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
