package me.snippex.rxtest

import io.reactivex.Observable
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ObservableBasicTest {

    @Test
    fun delay() {

        // RESULTS
        // =======
        //  [main] delay.before delay : OnNextNotification[10]
        //  [main] delay.before delay : OnCompleteNotification
        // << 1초 딜레이 >>
        //  [RxComputationThreadPool-1] delay.after delay : OnNextNotification[10]
        //  [RxComputationThreadPool-1] delay.subscribe : 10
        //  [RxComputationThreadPool-1] delay.after delay : OnCompleteNotification

        val latch = CountDownLatch(1)
        Observable.just(10)
                .doOnEach { pp("delay.before delay : $it") }
                .delay(1, TimeUnit.SECONDS)
                .doOnEach { pp("delay.after delay : $it") }
                .doOnComplete { latch.countDown() }
                .subscribe { pp("delay.subscribe : $it") }

        latch.await()
    }

    @Test
    fun blockingForEach_takeWhile() {

        // RESULTS
        // =======
        //  [main] 0
        //  [main] 1
        //  [main] 2
        //  [main] 3
        //
        // 관전 포인트
        // ========
        // CountDownLatch 를 사용하지 않고 blockingForEach 를 사용하여 테스트
        // takeWhile, takeUntil 에 대한 차이 (헷갈리니까 왠만하면 takeWhile 을 쓰는게 편리하겠다.)
        //

        Observable.interval(0, 500, TimeUnit.MILLISECONDS)
                .takeWhile { it < 5 }
                .takeUntil { it > 2 }
                .blockingForEach { pp("$it") }
    }

    @Test
    fun repeat() {

        // RESULTS
        // =======
        //  [main] 1
        //  [main] 2
        //  [main] 3
        //  [main] 1
        //  [main] 2
        //  [main] 3
        //
        generate("1,2,3", 500)
                .repeat(2)
                .blockingForEach { pp("$it") }
    }

    @Test
    fun scan() {

        // [SCAN]
        // http://reactivex.io/documentation/ko/operators/scan.html
        //
        // RESULTS
        // =======
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[0]
        //  [main] subscribe : 0
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[1]
        //  [RxComputationThreadPool-1] scan => 0 + 1
        //  [main] subscribe : 1
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[2]
        //  [RxComputationThreadPool-1] scan => 1 + 2
        //  [main] subscribe : 3
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[3]
        //  [RxComputationThreadPool-1] scan => 3 + 3
        //  [main] subscribe : 6
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[4]
        //  [RxComputationThreadPool-1] scan => 6 + 4
        //  [main] subscribe : 10
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[5]
        //  [RxComputationThreadPool-1] scan => 10 + 5
        //  [main] subscribe : 15
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[6]
        //  [RxComputationThreadPool-1] scan => 15 + 6
        //
        // 관전 포인트
        // ========
        // scan 이 발생하는 쓰레드
        // scan 이 최초로 실행되는 순간 (2개 이상 들어와야 아래로 흘러감)
        //
        Observable.interval(100, TimeUnit.MILLISECONDS)
                .doOnEach { pp("doOnEach : $it") }
                .scan { acc: Long, value: Long ->
                    pp("scan => $acc + $value")
                    acc + value
                }
                .takeWhile { it < 20 }
                .blockingForEach { pp("subscribe : $it") }
    }

    @Test
    fun scanWithInitialValue() {

        //  [main] subscribe : 5
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[0]
        //  [RxComputationThreadPool-1] scan => 5 + 0
        //  [main] subscribe : 5
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[1]
        //  [RxComputationThreadPool-1] scan => 5 + 1
        //  [main] subscribe : 6
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[2]
        //  [RxComputationThreadPool-1] scan => 6 + 2
        //  [main] subscribe : 8
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[3]
        //  [RxComputationThreadPool-1] scan => 8 + 3
        //  [main] subscribe : 11
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[4]
        //  [RxComputationThreadPool-1] scan => 11 + 4
        //  [main] subscribe : 15
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[5]
        //  [RxComputationThreadPool-1] scan => 15 + 5
        //
        // seed 값이 subscription 과 동시에 하류로 흘러내려가는 것 확인
        // scanWith 를 이용하면 함수를 사용할 수 있으나 initial value 가 없는 것은 동일함.

        Observable.interval(100, TimeUnit.MILLISECONDS)
                .doOnEach { pp("doOnEach : $it") }
                .scan(5) { acc: Long, value: Long ->
                    pp("scan => $acc + $value")
                    acc + value
                }
                .takeWhile { it < 20 }
                .blockingForEach { pp("subscribe : $it") }
    }
}

