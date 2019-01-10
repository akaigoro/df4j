#version 7.0

org.reactivestreams.* interfaces used for connectors of all protocols

#version 6.3

### CHANGES

1. ScalarCollector renamed to ScalarSubscriber and StreamCollector renamed to StreamSubscriber. Thus, old ScalarSubscriber and StreamSubscriber effectively removed.
StreamPublisher.subscribe and ScalarPublisher.subscribe return subscription.

2, ScalarSubscriber.complete renamed to ScalarSubscriber.post and returns void. Similary, ScalarSubscriber.completeExceptionally renamed to postFailure.

3. AsyncAction#runAction returns void. If a non-void result requires, use new method AsyncAction#callAction.

### ADDITIONS

1. package org.df4j.core.util.asyncmon, with support to convert synchronous multithreading program to asynchronous.

2. Class org.df4j.core.util.TimeSignalPublisher, which converts Timer into source of permits.
