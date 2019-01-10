/**
 * <pre>
 * this package allows mechanically convert synchronized methods used in multithreaded programs
 * to asynchronous procedures. The scenario is as follows:
 *
 *  - make the object with synchronized methods to extend {@link org.df4j.core.util.asyncmon.AsyncObject}
 *
 *  - a method which returns void is converted like this:
 *          public synchronized void put(some arguments) throws InterruptedException {
 *              body
 *          }
 *    to
 *          public void put(same arguments, {@link org.df4j.core.boundconnector.permitscalar.ScalarPermitSubscriber} connector) {
 *             super.exec(({@link org.df4j.core.util.asyncmon.AsyncMonitor} monitor) -&gt; {
 *                 body
 *             );
 *         }
 *
 *  - a method which returns value is converted like this:
 *          public synchronized T get(some arguments) throws InterruptedException {
 *              body
 *              return value
 *          }
 *    to
 *          public T get(same arguments, {@link org.df4j.core.boundconnector.permitscalar.ScalarPermitSubscriber}&lt;T&gt; connector) {
 *             super.exec(({@link org.df4j.core.util.asyncmon.AsyncMonitor} monitor) -&gt; {
 *                 body
 *                 connector.onNext(value);
 *             );
 *         }
 *
 *   in both cases, synchronization operations in the body are converted as follows:
 *             while (condition) {
 *                 wait();
 *             }
 *    is converted to
 *             if (condition) {
 *                  monitor.doWait();
 *                  return;
 *             }
 *    and
 *             notify();
 *             notifyAll();
 *    are converted to
 *             monitor.doNotify();
 *             monitor.doNotifyAll();
 *
 *    See an example in the test section of this modeule.
 * </pre>
 */
package org.df4j.core.util.asyncmon;