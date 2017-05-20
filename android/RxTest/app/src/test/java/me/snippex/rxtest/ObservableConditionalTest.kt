package me.snippex.rxtest

import io.reactivex.Observable
import io.reactivex.functions.BiPredicate
import org.junit.Test
import java.util.concurrent.TimeUnit

class ObservableConditionalTest {

    @Test
    fun all() {
        generate("1,2,3", 100)
                .all { it > 0 }
                .test().await().assertValue(true)
    }

    @Test
    fun amb() {
        //  [00:19.903] [RxComputationThreadPool-3] stream3 emitting : 30
        //  [00:19.914] [RxComputationThreadPool-3] stream1 disposed
        //  [00:19.914] [RxComputationThreadPool-3] stream2 disposed
        //  [00:19.915] [main] 30
        //  [00:19.951] [RxComputationThreadPool-3] stream3 emitting : 31
        //  [00:19.951] [main] 31
        //  [00:20.004] [RxComputationThreadPool-3] stream3 emitting : 32
        //  [00:20.005] [main] 32

        val races = listOf(
                Observable.intervalRange(1, 3, 500, 100, TimeUnit.MILLISECONDS)
                        .doOnDispose { pp("stream1 disposed") }
                        .printNext("stream1 emitting : "),
                Observable.intervalRange(10, 3, 300, 30, TimeUnit.MILLISECONDS)
                        .doOnDispose { pp("stream2 disposed") }
                        .printNext("stream2 emitting : "),
                Observable.intervalRange(30, 3, 200, 50, TimeUnit.MILLISECONDS)
                        .printNext("stream3 emitting : ")
        )

        Observable.amb(races).print()
        Observable.timer(2, TimeUnit.SECONDS).testResults(0)

        Observable.just(1).delay(300, TimeUnit.MILLISECONDS)
                .ambWith(Observable.just(2).delay(100, TimeUnit.MILLISECONDS))
                .testResults(2)
    }

    @Test
    fun contains() {
        generate("1,2,3", 10)
                .contains(4)
                .test().await().assertValue(false)
    }

    @Test
    fun defaultIfEmpty() {
        generate("", 10)
                .defaultIfEmpty(100)
                .testResults(100)
    }

    @Test
    fun sequenceEqual() {
        val source1 = generate("1,2,3", 50)
        val source2 = generate("-1,-2,-3", 70)

        Observable.sequenceEqual(source1, source2, BiPredicate { v1, v2 -> Math.abs(v1) == Math.abs(v2) })
                .test().await().assertValue(true)
    }

    @Test
    fun skipUntil_takeUntil_takeWhile_etc() {
        generate("1,2,3", 100)
                .skipUntil(Observable.timer(150, TimeUnit.MILLISECONDS))
                .testResults(3)

        generate("1,2,3", 50)
                .takeUntil(Observable.timer(80, TimeUnit.MILLISECONDS))
                .testResults(1, 2)

        generate("1,2,4,1,3", 50)
                .takeUntil { it > 3 }
                .testResults(1, 2, 4) // NOTE: 4는 포함된다.

        generate("1,2,4,1,3", 50)
                .takeWhile { it < 3 }
                .testResults(1, 2) // NOTE: 4는 미포함된다.

        Observable.just(1, 2, 4, 1, 3)
                .skipWhile { it < 3 }
                .testResults(4, 1, 3)

        Observable.just(1, 2, 4, 1, 3)
                .takeLast(2)
                .testResults(1, 3)

        generate("1,2,3,4,5", 50)
                .takeLast(80, TimeUnit.MILLISECONDS)
                .testResults(4, 5)

        generate("1,2,3,4,5", 100)
                .take(150, TimeUnit.MILLISECONDS)
                .testResults(1, 2)
    }

}

