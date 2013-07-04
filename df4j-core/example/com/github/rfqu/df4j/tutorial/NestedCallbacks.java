package com.github.rfqu.df4j.tutorial;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;

import org.junit.Test;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Timer;
import com.github.rfqu.df4j.ext.Request;

/** Demonstrates solution to the StackOverflow question 
 * http://stackoverflow.com/questions/10695152/java-pattern-for-nested-callbacks/10695933
 * This solution differs in details:
 *  - Request is a standard df4j class, not interface.
 *  - Response is not an interface, but a type parameter in Request, and 
 *    response itself id returned inside the original request
 *  - Callback is standard df4j interface, so method names differ
 *  - Callback is used in Client class only, services exchange with
 *    instances of class Response
 *    
 *  The main feature of the df4j framework which allowed to program
 *  this task is ability of Actors to have additional inputs, in our case of type Sema.
 */
public class NestedCallbacks {
    static PrintStream out = System.out;

    Service[] services = new Service[10];
    LongService[] longServices = new LongService[10];
    {
        for (int k = 0; k < services.length; k++) {
            services[k] = new Service(k);
        }
        for (int k = 0; k < longServices.length; k++) {
            longServices[k] = new LongService(k);
        }
    }
    Random rand = new Random();
    CountDownLatch sink;

    LongService randomLongService() {
        return longServices[rand.nextInt(longServices.length)];
    }

    Service randomService() {
        return services[rand.nextInt(services.length)];
    }

    /** Requests carry some data and return address
     */
    static class RequestS extends Request<RequestS, Integer> {
        private StringBuilder log;
        
        public RequestS(String value) {
            this.log = new StringBuilder(value);
            log(value);
        }
        
        public RequestS(StringBuilder log) {
            this.log = log;
        }
        
        public void addLog(String value) {
            log.append(value);
            log(log.toString());
        }

        public StringBuilder getLog() {
            return log;
        }
    }

    /**
     * demonstrates how to handle long computations asynchronously:
     *  - extract the "tail" of long procedure into a post method
     * of a separate object which implements Port
     *  - set that port as listener in the request  
     *  - send request to service. The service should just call request.post(result).
     */
    class Client implements Callback<Integer> {
        int id;

        public Client(int id) {
            this.id = id;
        }

        void start() {
            RequestS requestA = new RequestS("<Client:" + id);
            requestA.addListener(finish);
            randomService().post(requestA);
        }

        Port<RequestS> finish = new Port<RequestS>() {

            @Override
            public void post(RequestS responseA) {
                responseA.addLog(", Client:" + id+">");
                responseA.toCallback(Client.this);
                sink.countDown();
            }

        };

        // implements Callback
        
        @Override
        public void post(Integer m) {
            log("Client:" + id+" time:"+m.toString());
        }

        @Override
        public void postFailure(Throwable exc) {
            log("Client:" + id+" failed:" + exc);
        }
    }

    /** first layer service
     * To process input request, it sends 2 sequential requests to LongService
     */
    class Service extends Actor<RequestS> {
        Semafor running = new Semafor(); // closed when long request is executed
        private int id;
        RequestS requestA;
        long start;

        public Service(int id) {
            this.id = id;
            running.up();
        }

        @Override
        protected void act(RequestS requestA) throws Exception {
            this.requestA = requestA;
            requestA.addLog(", <Service:" + id);
            RequestS requestB1 = new RequestS(requestA.getLog());
            requestB1.addListener(callbackB1);
            start=System.currentTimeMillis();
            randomLongService().post(requestB1);
        }

        Port<RequestS> callbackB1 = new Port<RequestS>() {
            @Override
            public void post(RequestS requestB1) {
                RequestS requestB2 = new RequestS(requestB1.getLog());
                requestB2.addLog(", Service:" + id);
                requestB2.addListener(callbackB2);
                randomLongService().post(requestB2);
            }
        };

        Port<RequestS> callbackB2 = new Port<RequestS>() {
            @Override
            public void post(RequestS requestB2) {
                requestB2.addLog(", Service:" + id + ">");
                requestA.post((int)(System.currentTimeMillis()-start));
                running.up(); // last callback opens the semaphore
            }
        };
    }

    /** to imitate long work, class Timer is used, which echoes
     * a message over given period of time.
     */
    class LongService extends Actor<RequestS> {
        Semafor running = new Semafor(); // closed when long request is executed
        private int id;
        int delay;
        
        public LongService(int id) {
            this.id = id;
            running.up(); // allow reaction to one incoming message
        }

        @Override
        protected void act(RequestS requestB) throws Exception {
            requestB.addLog(", <LS:" + id);
            Timer timer = Timer.getCurrentTimer();
            delay = rand.nextInt(400) + 200; // milliseconds
            // TODO cancel ttask
            @SuppressWarnings("unused")
            ScheduledFuture<?> ttask = timer.schedule(callbackB, requestB, delay);
            //ttask.cancel(false);
        }

        Port<RequestS> callbackB = new Port<RequestS>() {
            @Override
            public void post(RequestS requestB) {
                requestB.addLog(", LS:" + id + ">");
                requestB.post(delay);
                running.up(); // allow reaction to another incoming message
            }
        };
    }

//////////// tests
    
    @Test
    public void lsTest() throws Exception, IOException, InterruptedException {
        final CountDownLatch sink = new CountDownLatch(1);
        Port<RequestS> callbackB1 = new Port<RequestS>() {
            @Override
            public void post(RequestS requestB) {
                requestB.addLog(", lsTest>");
                sink.countDown();
            }
        };
        RequestS requestB = new RequestS("<lsTest ");
        requestB.addListener(callbackB1);
//        randomLongService().post(requestB);
        LongService ls1=new LongService(1);
        ls1.post(requestB);
        sink.await();
        log("time="+requestB.getResult());
    }
    
    @Test
    public void sTest() throws Exception, IOException, InterruptedException {
        final CountDownLatch sink = new CountDownLatch(1);
        Port<RequestS> callbackB1 = new Port<RequestS>() {
            @Override
            public void post(RequestS requestB) {
                requestB.addLog(", sTest>");
                sink.countDown();
            }
        };
        RequestS requestB = new RequestS("<sTest ");
        requestB.addListener(callbackB1);
        randomService().post(requestB);
        sink.await();
        log("time="+requestB.getResult());
    }

    @Test
    public void smokeTest() throws Exception, IOException, InterruptedException {
        runTest(1);
    }

    @Test
    public void mediumTest() throws Exception, IOException, InterruptedException {
        runTest(5);
    }

    void runTest(int numClients) throws InterruptedException {
        out.println("======= numClients:" + numClients);
        sink = new CountDownLatch(numClients);
        for (int k = 0; k < numClients; k++) {
            Client client = new Client(k);
            client.start();
        }
        sink.await(); // wait for all clients to finish
    }

    static void log(String string) {
        out.println(string);
    }

    public static void main(String[] args) throws Exception {
        NestedCallbacks t = new NestedCallbacks();
        t.smokeTest();
    }

}
