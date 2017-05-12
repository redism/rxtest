package me.snippex.rxtest

import org.junit.Test

class TestObserverTest {

    @Test
    fun testObserver() {
        generate("1,2,3", 200)
                .test()
                .await()
                .assertResult(1, 2, 3) // check termination
                .assertValues(1, 2, 3)
                .assertValueSequence(listOf(1, 2, 3))
                .assertValueSet(setOf(1, 2, 3, 4))
                .assertValueCount(3)
                .assertValueAt(1, { it == 2 })
                .assertComplete()
                .assertTerminated()
                .assertNoErrors()

        generate("1,2,3", 200)
                .test()
                .awaitCount(2)
//                .assertResult(1, 2) // 이 테스트는 성공할 수 없다.
                .assertValues(1, 2)
                .awaitCount(3)
                .assertResult(1, 2, 3)
    }
}

