
/**
 * Echo server, connections are represented as pair of IOHandlers. 
 * Running this requires that an implementation of {@link com.github.rfqu.pipeline.nio.AsyncChannelFactory}
 * be present in the classpath {@see com.github.rfqu.df4j.nio.AsyncChannelFactory#factoryClassNames}.
 * The easiest way to provide this is to run extention classes from df4j-nio1
 * or df4j-nio2.
 */
package org.df4j.nio2.net.echo;