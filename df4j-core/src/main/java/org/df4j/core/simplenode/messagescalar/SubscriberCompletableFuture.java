package org.df4j.core.simplenode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;

import java.util.concurrent.CompletableFuture;

public class SubscriberCompletableFuture<R> extends CompletableFuture<R> implements ScalarSubscriber<R> {
}
