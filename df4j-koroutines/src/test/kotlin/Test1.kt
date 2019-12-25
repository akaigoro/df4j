package kotlinx.coroutines.rx2

import io.reactivex.Flowable
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class Test1 {
@Test
fun givenAsyncCoroutine_whenStartIt_thenShouldExecuteItInTheAsyncWay() {
    // given
    val res = mutableListOf<String>()

    val promise = runBlocking() {
        expensiveComputation(res)
    }
    promise.join()
    // when
    runBlocking<Unit> {
    }
    assertEquals(listOf("Hello,", "word!"), res)
}

suspend fun expensiveComputation(res: MutableList<String>) {
    Flowable.fromArray("Hello,", "word!")
            .collect {
                res.add(it)
            }
}

}
