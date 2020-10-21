package org.df4j.reactor.actor;

import org.junit.Test;
import reactor.core.publisher.Flux;

public class OutFluxTest {
    @Test
    public void test1() {
        Flux<String> flux = Flux.generate(
                () -> 0,
                (state, sink) -> {
                    sink.next("3 x " + state + " = " + 3*state);
                    if (state == 10) sink.complete();
                    return state + 1;
                });
        flux.subscribe((s)->System.out.println(s));
    }
}
