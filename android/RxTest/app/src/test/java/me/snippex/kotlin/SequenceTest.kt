package me.snippex.kotlin

import io.reactivex.rxkotlin.toFlowable
import org.junit.Test

class SequenceTest {

    @Test
    fun sequenceAndLazyEvaluation() {
        // kotlin 의 sequence 와 observable 이 변환이 가능하다는 것도 재미있고,
        // take 를 앞에 두던 뒤에 두던 잘 된다는 것도 재미있는 것 같다.
        var current = 0
        val seq = generateSequence {
            println("generating, $current")
            current++
        }

        seq
//                .take(5)
                .map {
                    println("map > $it")
                    it
                }
                .asSequence()
                .toFlowable()
//                .take(5)
                .subscribe {
                    println("subscribe > $it")
                }

        Thread.sleep(500)
    }
}

