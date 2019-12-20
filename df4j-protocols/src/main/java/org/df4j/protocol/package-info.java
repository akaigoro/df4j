/**
 *
 *  A protocol describes following properties of a connection:
 *<ul>
 *   <li>what kind of tokens are transferred through the connection:
 *     <ul>
 *       <li>signals (pure events without value)</li>
 *       <li>completions (signals with possible errors)</li>
 *       <li>messages (events with value and possible errors)</li>
 *     </ul>
 *   </li>
 *   <li>how many tokens can be transferred through the lifetime of the connection: one or many. One-shot protocols are named <i>scalar</i> protocols.
 *   They assume that sender sends single token which is duplicated for each reciever (multicast style). Multi-shot protocols are named <i>stream</i> protocols.
 *   Each token is passed to exactly one receiver (unicast style). Multicast stream protocols are possible, but not defined and implemented in this library.</li>
 * </ul>
 *<table style="width:100%">
 *   <caption>Protocol List</caption>
 *   <tr>
 *     <th>Token Type</th>
 *     <th>Scalar protocol name</th>
 *     <th>Stream protocol name</th>
 *   </tr>
 *   <tr>
 *     <td>Signal</td>
 *     <td>{@link org.df4j.protocol.Signal}</td>
 *     <td>{@link org.df4j.protocol.SignalStream}</td>
 *   </tr>
 *   <tr>
 *     <td>Completion</td>
 *     <td>{@link org.df4j.protocol.Completion}</td>
 *     <td>N/A</td>
 *   </tr>
 *   <tr>
 *     <td>Message</td>
 *     <td>{@link org.df4j.protocol.ScalarMessage}</td>
 *     <td>{@link org.df4j.protocol.MessageStream}, {@link org.df4j.protocol.MessageChannel}</td>
 *   </tr>
 * </table>
 *
 */
package org.df4j.protocol;