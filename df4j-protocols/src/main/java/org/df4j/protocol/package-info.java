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
 *
 *   <li>how many tokens can be transferred through the lifetime of the connection: one or many.
 *   <p>
 *   One-shot protocols are named <i>scalar</i> protocols.
 *   They assume that sender sends single token which is duplicated for each reciever (multicast style).
 *   Unicast scalar protocols are possible, but have little sense and are not defined and implemented in this library.
 *   <p>
 *   Multi-shot protocols are named <i>flow</i> protocols.
 *   Each token is passed to exactly one receiver (unicast style).
 *   Multicast flow protocols are possible, but not defined and implemented in this library.</li>
 * </ul>
 *<table style="width:100%">
 *   <caption><b>Protocol List</b></caption>
 *   <tr>
 *     <th> </th>
 *     <th> </th>
 *     <th> </th>
 *   </tr>
 *   <tr>
 *     <td><b>Token Type</b></td>
 *     <td><b>Scalar protocol name</b></td>
 *     <td><b>Flow protocol name</b></td>
 *   </tr>
 *   <tr>
 *     <td>Signal</td>
 *     <td><i>not declared</i></td>
 *     <td>{@link org.df4j.protocol.SignalFlow}</td>
 *   </tr>
 *   <tr>
 *     <td>Completion</td>
 *     <td>{@link org.df4j.protocol.Completable}</td>
 *     <td><i>not declared</i></td>
 *   </tr>
 *   <tr>
 *     <td>Message</td>
 *     <td>{@link org.df4j.protocol.Scalar}</td>
 *     <td>{@link org.df4j.protocol.Flood}, {@link org.df4j.protocol.Flow}, {@link org.df4j.protocol.ReverseFlow}</td>
 *   </tr>
 * </table>
 *
 */
package org.df4j.protocol;