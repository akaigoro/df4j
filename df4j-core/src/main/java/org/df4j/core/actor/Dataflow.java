package org.df4j.core.actor;

import org.df4j.core.communicator.Completable;
import org.df4j.core.util.Utils;
import org.df4j.protocol.Completion;

import java.util.Timer;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A dataflow graph, consisting of 1 or more {@link BasicBlock}s and, probably, nested {@link Dataflow}s.
 * Completion signals (errors or success) propagate from the leaf nodes to the root node.
 * Component {@link BasicBlock}s plays the same role as basic blocks in a flow chart.
 */
public class Dataflow implements Activity, Completion.CompletableSource {
    protected Dataflow parent;
    protected Executor executor;
    protected Timer timer;
    protected Completable completionSignal = new Completable();
    protected int nodeCount = 0;

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
        this.parent = parent;
        parent.enter();
    }

    public synchronized void setExecutor(Executor executor) {
        this.executor = executor;
    }

    protected synchronized Executor getExecutor() {
        if (executor != null) {
            return executor;
        } else if (parent != null) {
            return parent.getExecutor();
        } else {
            return executor = Utils.getThreadLocalExecutor();
        }
    }

    public synchronized void setTimer(Timer timer) {
        this.timer = timer;
    }

    public synchronized Timer getTimer() {
        if (timer != null) {
            return timer;
        } else if (parent != null) {
            return timer = parent.getTimer();
        } else {
            timer = new Timer();
            return timer;
        }
    }

    /**
     * indicates that a node has added to this graph.
     */
    public synchronized void enter() {
        nodeCount++;
    }

    /**
     * indicates that a node has left this graph because of successful completion.
     * when all the nodes has left this graph, it is considered successfully completed itself
     * and leaves the pareng graph, if any.
     */
    public synchronized void leave() {
        if (nodeCount==0) {
            throw new IllegalStateException();
        }
        nodeCount--;
        if (nodeCount==0) {
            completionSignal.onComplete();
            if (parent != null) {
                parent.leave();
            }
        }
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAlive() {
        return !completionSignal.isCompleted();
    }

    protected void onError(Throwable t) {
        completionSignal.onError(t);
        if (parent != null) {
            parent.onError(t);
        }
    }

    public void subscribe(Completion.CompletableObserver co) {
        completionSignal.subscribe(co);
    }

    public boolean unsubscribe(Completion.CompletableObserver co) {
        return completionSignal.unsubscribe(co);
    }

    public void join() {
        completionSignal.blockingAwait();
    }

    public boolean blockingAwait(long timeout) {
        return completionSignal.blockingAwait(timeout);
    }

    public boolean blockingAwait(long timeout, TimeUnit unit) {
        return completionSignal.blockingAwait(timeout, unit);
    }

    public boolean isCompleted() {
        return completionSignal.isCompleted();
    }
}
