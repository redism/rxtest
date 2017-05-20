package me.snippex.rxtest

import io.reactivex.Observable
import org.junit.Test
import java.util.concurrent.TimeUnit

class ObservableAggregationTest {

    @Test
    fun concat() {
        val source1 = generate("1,2,3", 50)
        val source2 = generate("4,5,6", 10)

        Observable.concat(source1, source2)
                .testResults(1, 2, 3, 4, 5, 6)

        val sourceOfSources = Observable.intervalRange(1, 3, 0, 10, TimeUnit.MILLISECONDS)
                .map { value -> Observable.interval(30, TimeUnit.MILLISECONDS).map { value }.take(2) }

        Observable.concat(sourceOfSources)
                .testResults(1, 1, 2, 2, 3, 3)

        // concatArray(), delayError
    }

    @Test
    fun concatEager() {

        //  [31:27.615] [main] subscribe 1
        //  [31:27.631] [main] 1
        //  [31:27.682] [main] 2
        //  [31:27.732] [main] 3
        //  [31:27.732] [RxComputationThreadPool-3] subscribe 2
        //  [31:27.732] [main] 4
        //  [31:27.745] [main] 5
        //  [31:27.753] [main] 6
        //  [31:27.755] [main] =========
        //  [31:27.757] [main] subscribe 1
        //  [31:27.758] [main] subscribe 2
        //  [31:27.759] [main] 1
        //  [31:27.810] [main] 2
        //  [31:27.859] [main] 3
        //  [31:27.860] [main] 4
        //  [31:27.860] [main] 5
        //  [31:27.860] [main] 6

        val source1 = generate("1,2,3", 50).doOnSubscribe { pp("subscribe 1") }
        val source2 = generate("4,5,6", 10).doOnSubscribe { pp("subscribe 2") }

        Observable.concat(source1, source2)
                .print()

        pp("=========")
        Observable.concatArrayEager(source1, source2)
                .print()

        // concatArrayEager has maxConcurrency option, prefetch option
        // Observable source of observable sources 를 받는 concatEager 도 있음.
        // concatDelayError() 도 있음.

        generate("1,2,3", 50).concatWith(generate("4,5", 10))
                .testResults(1, 2, 3, 4, 5)
    }

    @Test
    fun count() {
        generate("1,2,3", 50)
                .count()
                .test().await().assertValue(3)
    }

    @Test
    fun reduce() {
        generate("1,2,3", 10)
                .reduce(100, { acc, v -> acc + v })
                .test().await().assertValue(106)

        // omitting seed value works like scan
        generate("1,2,3", 10)
                .printNext("emitting : ")
                .reduce({ acc, v ->
                    pp("reducer called $acc / $v")
                    acc + v
                })
                .test().await().assertValue(6)
    }
}

