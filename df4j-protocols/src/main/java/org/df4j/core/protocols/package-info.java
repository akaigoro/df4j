/**
 *
 *  A protocol defines:
 *
 *  - what kind of tokens are transferred through the connection: pure events or events with values. In terms of Petri Nets, these are black and colored tokens, respectively.
 *  - how many tokens can be transferred through the liftime of the connection: one or many
 *  - is backpressure supported
 */
package org.df4j.core.protocols;