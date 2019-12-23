package kotlinx.coroutines.rx2

import kotlinx.coroutines.CommonPool
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

@Test
fun givenAsyncCoroutine_whenStartIt_thenShouldExecuteItInTheAsyncWay() {
    // given
    val res = mutableListOf<String>()

    // when
    runBlocking<Unit> {
        val promise = launch(CommonPool) {
            expensiveComputation(res)
        }
        res.add("Hello,")
        promise.join()
    }

    // then
    assertEquals(res, listOf("Hello,", "word!"))
}

fun expensiveComputation(res: MutableList<String>) {


}
