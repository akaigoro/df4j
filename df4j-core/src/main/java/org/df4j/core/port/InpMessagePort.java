package org.df4j.core.port;

public interface InpMessagePort<T> {
    boolean isReady();
    T current();
    T remove();
    boolean isCompleted();
    Throwable getCompletionException();
}
/*
	isCancelled()
	isCompletedExceptionally()
    isDone() // Returns true if completed in any fashion: normally, exceptionally, or via cancellation.
 */
/*
Throws exception	Special value
Remove	remove()	poll()
Examine	element()	peek()
 */