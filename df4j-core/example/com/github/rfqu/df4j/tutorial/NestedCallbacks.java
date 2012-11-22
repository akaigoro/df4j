package com.github.rfqu.df4j.tutorial;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Request;
import com.github.rfqu.df4j.ext.Timer;

/** Demonstrates solution to the StackOverflow question 
 * http://stackoverflow.com/questions/10695152/java-pattern-for-nested-callbacks/10695933
 * This solution differs in details:
 *  - Request is a standard df4j class, not interface.
 *  - Response is not an interface, but a type parameter in Request, and 
 *    response itself id returned inside the original request
 *  - Callback is standard df4j class, so method names differ
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
    static class RequestA extends Request<RequestA, String> {

        public RequestA(Port<RequestA> callback, String value) {
            super(callback);
            this.result = value;
        }
    }

    static class RequestB extends Request<RequestB, String> {

        public RequestB(Port<RequestB> callback, String value) {
            super(callback);
            this.result = value;
        }
    }

    /** to imitate long work, class Timer is used, which echoes
     * a message over given period of time.
     */
    class LongService extends Actor<RequestB> {
        Sema running = new Sema(); // closed when long request is executed
        private int id;

        public LongService(int id) {
            this.id = id;
            running.up(); // allow reaction to one incoming message
        }

        @Override
        protected void act(RequestB requestB) throws Exception {
            Timer timer = Timer.getCurrentTimer();
            requestB.setResult(requestB.getResult() + (", <LS:" + id));
            log(requestB.getResult());
            int delay = rand.nextInt(400) + 200; // milliseconds
            timer.schedule(callbackB, requestB, delay);
        }

        Port<RequestB> callbackB = new Port<RequestB>() {
            @Override
            public void send(RequestB requestB) {
                requestB.setResult(requestB.getResult() + ", LS:" + id + ">");
                log(requestB.getResult());
                requestB.reply();
                running.up(); // allow reaction to another incoming message
            }
        };
    }

    class Service extends Actor<RequestA> {
        Sema running = new Sema(); // closed when long request is executed
        private int id;
        RequestA requestA;

        public Service(int id) {
            this.id = id;
            running.up();
        }

        @Override
        protected void act(RequestA requestA) throws Exception {
            this.requestA = requestA;
            RequestB requestB = new RequestB(callbackB1, requestA.getResult() + ", <Service:" + id);
            log(requestB.getResult());
            randomLongService().send(requestB);
        }

        Port<RequestB> callbackB1 = new Port<RequestB>() {
            @Override
            public void send(RequestB requestB) {
                requestB.setResult(requestB.getResult() + ", callbackB1");
                log(requestB.getResult());
                requestB.setReplyTo(callbackB2);
                randomLongService().send(requestB);
            }
        };

        Port<RequestB> callbackB2 = new Port<RequestB>() {
            @Override
            public void send(RequestB requestB) {
                requestB.setResult(requestB.getResult() + ", callbackB2");
                log(requestB.getResult());
                requestB.setReplyTo(callbackB3);
                randomLongService().send(requestB);
            }
        };

        Port<RequestB> callbackB3 = new Port<RequestB>() {
            @Override
            public void send(RequestB requestB) {
                requestA.setResult(requestB.getResult() + ", Service:" + id + ">");
                log(requestA.getResult());
                requestA.reply();
                running.up(); // last callback opens the semaphore
            }
        };
    }

    class Client implements Callback<String> {
        int id;

        public Client(int id) {
            this.id = id;
        }

        void start() {
            RequestA requestA = new RequestA(callbackA, "<Client:" + id);
            log(requestA.getResult());
            randomService().send(requestA);
        }

        Port<RequestA> callbackA = new Port<RequestA>() {

            @Override
            public void send(RequestA responseA) {
                responseA.toCallback(Client.this);
                sink.countDown();
            }

        };

        // implements Callback
        
        @Override
        public void send(String m) {
            log(m + ", Client:" + id + ">");
        }

        @Override
        public void sendFailure(Throwable exc) {
            log("failed:" + exc + ", Client:" + id);
        }
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
