package org.df4j.reactor.dataflow;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.port.InpFlow;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public  class ZipActorTest {

    abstract class ZipActor<T1, T2> extends Actor {
        InpFlow<T1> inpFlow = new InpFlow<>(this);
        InpFlow<T2> inpScalar = new InpFlow<>(this);

        ZipActor(Flux<T1> flux, Mono<T2> mono) {
            flux.subscribe(inpFlow);
            mono.subscribe(inpScalar);
        }

        @Override
        protected void runAction() throws Throwable {
            if (inpFlow.isCompleted()) {
                onComplete();
                return;
            }
            T1 element1 = inpFlow.removeAndRequest();
            T2 element2 = inpScalar.current();
            runAction(element1, element2);
        }

        protected abstract void runAction(T1 element1, T2 element2);
    }

    @Test
    public void ZipActorTest() throws InterruptedException {
        Flux<Integer> flux = Flux.just(1,2,3);
        Mono<Integer> mono = Mono.just(5);
        ZipActor<Integer, Integer> actor = new ZipActor<Integer, Integer>(flux, mono){
            @Override
            protected void runAction(Integer element1, Integer element2) {
                System.out.println("got:"+element1+" and:"+element2);
            }
        };
        actor.start();
        actor.join();
    }

}