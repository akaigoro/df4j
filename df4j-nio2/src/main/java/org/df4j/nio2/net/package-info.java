/**
 * Asynchronous interface to NIO2 network API
 *
 *  3 basic classes:
 *
 *   - {@link org.df4j.nio2.net.ClientSocketPort} - asynchronously connects to a server,
 *      publishes single {@link java.nio.channels.AsynchronousSocketChannel}
 *
 *   - {@link org.df4j.nio2.net.ServerSocketPort} - accepts client connections and
 *      publishes {@link java.nio.channels.AsynchronousSocketChannel}s for each client.
 *
 *   - {@link org.df4j.nio2.net.Connection} - performs asynchronous reading and writing of {@link java.nio.ByteBuffer}s
 *     through a {@link java.nio.channels.AsynchronousSocketChannel}.
 */
package org.df4j.nio2.net;