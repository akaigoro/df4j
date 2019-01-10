/**
 * This package contain interfaces to support unbounded stream of messages.
 * Each stream contains any number of calls to {@link org.df4j.core.boundconnector.messagestream.Subscriber#onNext(java.lang.Object)}
 * and finishes with either call to {@link org.df4j.core.boundconnector.messagestream.Subscriber#onError(java.lang.Throwable)} }
 * or {@link org.df4j.core.boundconnector.messagestream.Subscriber#complete()} }
 */
package org.df4j.core.boundconnector.messagestream;

