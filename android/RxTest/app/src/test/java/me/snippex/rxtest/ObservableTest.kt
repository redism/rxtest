package me.snippex.rxtest

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ObservableTest {

    fun <T> generateObservable(list: List<T?>, delayInMS: Long = 1000): Observable<T> {
        return Observable.defer {
            val observables = arrayListOf<Observable<T>>()

            list.forEachIndexed { index, value ->
                value?.let {
                    observables.add(Observable.just(value).delay(index * delayInMS, TimeUnit.MILLISECONDS))
                }
            }
            Observable.merge(observables.asIterable())
        }
    }

    fun generate(streamString: String, delayInMS: Long = 1000): Observable<Int> {
        return generateObservable(streamString.split(",").map { it.takeIf { it.isNotEmpty() }?.toIntOrNull() }, delayInMS)
    }

    @Test
    fun testGenerateObservable() {
        val latch = CountDownLatch(1)
        generate("1,2,3,,,,4,5")
                .doOnComplete { latch.countDown() }
                .subscribe { pp("subscribe $it") }

        latch.await()
    }

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

    @Test
    fun buffer() {

        //  [main] buffer.subscribe : [1, 2], java.util.ArrayList
        //  [main] buffer.subscribe : [3, 4], java.util.ArrayList
        //  [main] buffer.subscribe : [5], java.util.ArrayList
        //
        // buffer 가 기본적으로 ArrayList 형태로 반환하며, complete 이 되면 다 차지 않더라도 반환

        generate("1,2,3,4,5", 200)
                .buffer(2)
                .blockingForEach { pp("buffer.subscribe : $it, ${it.javaClass.name}") }
    }

    @Test
    fun bufferWithBoundary() {

        //  [main] bufferWithBoundary.subscribe : [1, 2, 3]
        //  [main] bufferWithBoundary.subscribe : [4, 5, 6]
        //  [main] bufferWithBoundary.subscribe : [7, 8, 9]
        //  [main] bufferWithBoundary.subscribe : [10]
        //
        // Observable 자체를 buffer 의 조건으로 쓸 수 있는 놀라움을 경험

        val bufferBoundarySupplierObservable = Observable.interval(300, TimeUnit.MILLISECONDS)

        generate("1,2,3,4,5,6,7,8,9,10", 100)
                .buffer(bufferBoundarySupplierObservable)
                .blockingForEach { pp("bufferWithBoundary.subscribe : $it") }
    }

    @Test
    fun bufferWithSkip() {

        //  [main] bufferWithBoundary.subscribe : [1, 2]
        //  [main] bufferWithBoundary.subscribe : [2, 3]
        //  [main] bufferWithBoundary.subscribe : [3, 4]
        //  [main] bufferWithBoundary.subscribe : [4, 5]
        //  [main] bufferWithBoundary.subscribe : [5]
        //
        // Overlapping 하면서 buffering 을 수행할 수 있는 기능을 확인할 수 있다. 주석을 이용해서 skip 의 명확한 용도를 확인하자.

        generate("1,2,3,4,5", 100)
                .buffer(2, 1)
                .blockingForEach { pp("bufferWithSkip.subscribe : $it") }
    }

    @Test
    fun bufferWithSupplier() {

        //  [main] bufferWithSupplier.subscribe : [1, 2, 3], java.util.LinkedHashSet
        //  [main] bufferWithSupplier.subscribe : [3, 4, 5], java.util.LinkedHashSet
        //  [main] bufferWithSupplier.subscribe : [4, 5], java.util.LinkedHashSet
        //  [main] bufferWithSupplier.subscribe : [4, 5], java.util.LinkedHashSet
        //
        // Collection 타입의 bufferSupplier 를 생성하여 재미있는 일들을 수행할 수 있다는 것을 확인한다.
        // 스트림을 타는 데이터의 갯수가 아니라 컬렉션에 포함된 갯수를 이용하여 emit 됨을 확인한다.
        // skip 파라메터는 이전 컬렉션이 모두 채워졌는지 여부를 떠나 upstream 의 데이터 갯수만으로 정의됨을 확인한다.

        generate("1,2,3,3,4,5,4,5", 100)
                .buffer(3, 2, { mutableSetOf<Int>() })
                .blockingForEach { pp("bufferWithSupplier.subscribe : $it, ${it.javaClass.name}") }
    }

    @Test
    fun bufferWithTimeSpan() {

        //  [main] bufferWithSupplier.subscribe : [1, 2, 3]
        //  [main] bufferWithSupplier.subscribe : [3, 3, 4]
        //  [main] bufferWithSupplier.subscribe : [4, 5, 4]
        //  [main] bufferWithSupplier.subscribe : [4, 5]
        //
        // 많이 쓸 것만 같은 시간을 이용한 유틸리티 기능이 있음을 확인한다.

        generate("1,2,3,3,4,5,4,5", 100)
                .buffer(300, 200, TimeUnit.MILLISECONDS)
                .blockingForEach { pp("bufferWithSupplier.subscribe : $it") }
    }

    @Test
    fun bufferWithTimeSpanWithScheduler() {

        //  [main] supplying mutableSet as a buffer.
        //  [RxNewThreadScheduler-1] supplying mutableSet as a buffer.
        //  [RxNewThreadScheduler-1] <= buffered item thread
        //  [main] bufferWithSupplier.subscribe : [1, 2, 3]
        //  [RxNewThreadScheduler-1] supplying mutableSet as a buffer.
        //  [RxNewThreadScheduler-1] <= buffered item thread
        //  [main] bufferWithSupplier.subscribe : [3, 4]
        //  [RxNewThreadScheduler-1] supplying mutableSet as a buffer.
        //  [RxNewThreadScheduler-1] <= buffered item thread
        //  [main] bufferWithSupplier.subscribe : [4, 5]
        //  [RxComputationThreadPool-4] <= buffered item thread
        //  [main] bufferWithSupplier.subscribe : [4, 5]
        //  [RxComputationThreadPool-4] <= buffered item thread
        //
        // rx 는 어떤 쓰레드에서 작업을 처리해야 하는지에 대한 참 다양한 기능들을 제공하고 있음을 확인한다.
        // 다만, 마지막에 computation thread 로 튀는데 이 부분은 예상치 못한 부분이므로 체크가 필요하다.

        generate("1,2,3,3,4,5,4,5", 100)
                .buffer(300, 200, TimeUnit.MILLISECONDS, Schedulers.newThread(), {
                    pp("supplying mutableSet as a buffer.")
                    mutableSetOf<Int>()
                })
                .doOnEach { pp("<= buffered item thread") }
                .blockingForEach { pp("bufferWithSupplier.subscribe : $it") }
    }

    @Test
    fun bufferWithTimeSpanMaxCount() {

        //  [main] bufferTimeFixedInterval.subscribe : [1, 2, 3]
        //  [main] bufferTimeFixedInterval.subscribe : [3]
        //  [main] bufferTimeFixedInterval.subscribe : [4, 5, 4]
        //  [main] bufferTimeFixedInterval.subscribe : [5]
        //
        // Timespan or maxCount 임을 유의한다.
        // 다음과 같은 추가적인 signature 도 있다.
        //   public final Observable<List<T>> buffer(long timespan, TimeUnit unit, Scheduler scheduler, int count)

        generate("1,2,3,3,4,5,4,5", 50)
                .buffer(200, TimeUnit.MILLISECONDS, 3)
                .blockingForEach { pp("bufferWithTimeSpanMaxCount.subscribe : $it") }
    }

    @Test
    fun bufferWithTimeSpanRestartOnMaxCount() {

        //  [main] bufferWithTimeSpanMaxCount.subscribe : [1, 2, 3]
        //  [main] bufferWithTimeSpanMaxCount.subscribe : [3, 4, 5]
        //  [main] bufferWithTimeSpanMaxCount.subscribe : [4, 5]
        //
        // bufferWithTimeSpanMaxCount 에서 달라진 것을 확인한다.

        generate("1,2,3,3,4,5,4,5", 50)
                .buffer(200, TimeUnit.MILLISECONDS, Schedulers.newThread(), 3, { arrayListOf<Int>() }, true)
                .blockingForEach { pp("bufferWithTimeSpanMaxCount.subscribe : $it") }
    }

    @Test
    fun bufferWithCustomOpeningAndClosing() {

        //  [RxComputationThreadPool-2] Emitting OnNextNotification[1]
        //  [RxComputationThreadPool-3] Emitting OnNextNotification[2]
        //  [RxComputationThreadPool-4] Emitting OnNextNotification[3]
        //  [RxComputationThreadPool-1] Emitting OnNextNotification[4]
        //  [RxComputationThreadPool-1] 100ms elapsed, opening /w value = OnNextNotification[0]
        //  [RxComputationThreadPool-1] closing indicator called /w value = 0
        //  [RxComputationThreadPool-2] Emitting OnNextNotification[5]
        //  [RxComputationThreadPool-3] Emitting OnNextNotification[6]
        //  [RxComputationThreadPool-3] 50ms elapsed, closing
        //  [main] bufferWithTimeSpanMaxCount.subscribe : [5, 6]
        //  [RxComputationThreadPool-4] Emitting OnNextNotification[7]
        //  [RxComputationThreadPool-1] Emitting OnNextNotification[8]
        //  [RxComputationThreadPool-1] 100ms elapsed, opening /w value = OnNextNotification[1]
        //  [RxComputationThreadPool-1] closing indicator called /w value = 1
        //  [RxComputationThreadPool-2] Emitting OnNextNotification[9]
        //  [RxComputationThreadPool-3] Emitting OnNextNotification[10]
        //  [RxComputationThreadPool-4] 50ms elapsed, closing
        //  [main] bufferWithTimeSpanMaxCount.subscribe : [9, 10]
        //  [RxComputationThreadPool-4] Emitting OnNextNotification[11]
        //  [RxComputationThreadPool-1] Emitting OnNextNotification[12]
        //  [RxComputationThreadPool-1] 100ms elapsed, opening /w value = OnNextNotification[2]
        //  [RxComputationThreadPool-1] closing indicator called /w value = 2
        //  [RxComputationThreadPool-2] Emitting OnNextNotification[13]
        //  [RxComputationThreadPool-3] Emitting OnNextNotification[14]
        //  [RxComputationThreadPool-1] 50ms elapsed, closing
        //  [main] bufferWithTimeSpanMaxCount.subscribe : [13, 14]
        //  [RxComputationThreadPool-4] Emitting OnNextNotification[15]
        //  [RxComputationThreadPool-1] Emitting OnNextNotification[16]
        //  [RxComputationThreadPool-1] 100ms elapsed, opening /w value = OnNextNotification[3]
        //  [RxComputationThreadPool-1] closing indicator called /w value = 3
        //  [RxComputationThreadPool-2] Emitting OnNextNotification[17]
        //  [RxComputationThreadPool-2] Emitting OnCompleteNotification
        //  [RxComputationThreadPool-2] 50ms elapsed, closing
        //  [main] bufferWithTimeSpanMaxCount.subscribe : [17]
        //
        // 이를 이용하면 뭔가 독특한 기능을 수행할 수 있겠다는 것을 확인한다.
        // 마지막 17은 completion / closing 과 함께 전달된다.
        // 만약 15까지만 emit 하면 opening 되지 않은 상태라서 즉시 종료됨을 확인한다.
        // opening 된 것이 closing 되지 않은 상태에서 overlapping 형태로 closing 하게 되면 문제가 발생함을 확인한다.
        //   bufferWithCustomOpeningAndClosingNotClosing() 테스트 확인

        val openEvery200Ms = Observable.interval(100, TimeUnit.MILLISECONDS)
                .doOnEach { pp("100ms elapsed, opening /w value = $it") }

        class ClosingIndicatorFunction : io.reactivex.functions.Function<Long, ObservableSource<Long>> {
            override fun apply(t: Long): ObservableSource<Long> {
                pp("closing indicator called /w value = $t")
                return Observable.interval(50, TimeUnit.MILLISECONDS)
                        .doOnEach { pp("50ms elapsed, closing") }
            }
        }

        generate("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17", 25)
                .doOnEach { pp("Emitting $it") }
                .buffer(openEvery200Ms, ClosingIndicatorFunction())
                .blockingForEach { pp("bufferWithTimeSpanMaxCount.subscribe : $it") }

    }

    @Test
    fun bufferWithCustomOpeningAndClosingNotClosing() {

        //  [RxComputationThreadPool-2] Emitting OnNextNotification[1]
        //  [RxComputationThreadPool-3] Emitting OnNextNotification[2]
        //  [RxComputationThreadPool-4] Emitting OnNextNotification[3]
        //  [RxComputationThreadPool-1] Emitting OnNextNotification[4]
        //  [RxComputationThreadPool-1] 100ms elapsed, opening /w value = OnNextNotification[0]
        //  [RxComputationThreadPool-1] closing indicator called /w value = 0
        //  [RxComputationThreadPool-2] Emitting OnNextNotification[5]
        //  [RxComputationThreadPool-3] Emitting OnNextNotification[6]
        //  [RxComputationThreadPool-4] Emitting OnNextNotification[7]
        //  [RxComputationThreadPool-1] Emitting OnNextNotification[8]
        //  [RxComputationThreadPool-1] 100ms elapsed, opening /w value = OnNextNotification[1]
        //  [RxComputationThreadPool-1] closing indicator called /w value = 1
        //  [RxComputationThreadPool-2] Emitting OnNextNotification[9]
        //  [RxComputationThreadPool-3] Emitting OnNextNotification[10]
        //  [RxComputationThreadPool-4] Emitting OnNextNotification[11]
        //  [RxComputationThreadPool-1] Emitting OnNextNotification[12]
        //  [RxComputationThreadPool-1] 100ms elapsed, opening /w value = OnNextNotification[2]
        //  [RxComputationThreadPool-1] closing indicator called /w value = 2
        //  [RxComputationThreadPool-1] 200ms elapsed, closing 0
        //  [RxComputationThreadPool-1] Closing indicator closed (id = 0)
        //  [main] bufferWithTimeSpanMaxCount.subscribe : [5, 6, 7, 8, 9, 10, 11, 12]
        //  [RxComputationThreadPool-2] Emitting OnNextNotification[13]
        //  [RxComputationThreadPool-3] Emitting OnNextNotification[14]
        //  [RxComputationThreadPool-4] Emitting OnNextNotification[15]
        //  [RxComputationThreadPool-4] Emitting OnCompleteNotification
        //  [RxComputationThreadPool-1] 100ms elapsed, opening /w value = OnNextNotification[3]
        //  [RxComputationThreadPool-1] closing indicator called /w value = 3
        //  [RxComputationThreadPool-2] 200ms elapsed, closing 1
        //  [RxComputationThreadPool-2] Closing indicator closed (id = 1)
        //  [main] bufferWithTimeSpanMaxCount.subscribe : [9, 10, 11, 12, 13, 14, 15]
        //  [RxComputationThreadPool-3] 200ms elapsed, closing 2
        //  [RxComputationThreadPool-1] 100ms elapsed, opening /w value = OnNextNotification[4]
        //  [main] bufferWithTimeSpanMaxCount.subscribe : [13, 14, 15]
        //  [RxComputationThreadPool-3] Closing indicator closed (id = 2)
        //  [RxComputationThreadPool-1] closing indicator called /w value = 4
        //  [RxComputationThreadPool-1] 100ms elapsed, opening /w value = OnNextNotification[5]
        //  [RxComputationThreadPool-1] closing indicator called /w value = 5
        //  [RxComputationThreadPool-4] 200ms elapsed, closing 3
        //  [RxComputationThreadPool-4] Closing indicator closed (id = 3)
        //  [main] bufferWithTimeSpanMaxCount.subscribe : []
        //  [RxComputationThreadPool-1] 100ms elapsed, opening /w value = OnNextNotification[6]
        //  [RxComputationThreadPool-1] closing indicator called /w value = 6
        //  [RxComputationThreadPool-1] 200ms elapsed, closing 4
        //  [RxComputationThreadPool-1] Closing indicator closed (id = 4)
        //  [main] bufferWithTimeSpanMaxCount.subscribe : []
        // << 계속 opening / closing 을 반복하면서 infinite loop >>
        //
        // 이건 버그일지도 모르겠다.

        val openEvery200Ms = Observable.interval(100, TimeUnit.MILLISECONDS)
                .doOnEach { pp("100ms elapsed, opening /w value = $it") }

        class ClosingIndicatorFunction : io.reactivex.functions.Function<Long, ObservableSource<Long>> {
            override fun apply(t: Long): ObservableSource<Long> {
                pp("closing indicator called /w value = $t")
                return Observable.interval(200, TimeUnit.MILLISECONDS)
                        .doOnDispose { pp("Closing indicator disposed (id = $t)") }
                        .doOnEach { pp("200ms elapsed, closing $t") }
            }
        }

        generate("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15", 25)
                .doOnEach { pp("Emitting $it") }
                .buffer(openEvery200Ms, ClosingIndicatorFunction())
                .blockingForEach { pp("bufferWithCustomOpeningAndClosingNotClosing.subscribe : $it") }

    }

    @Test
    fun bufferWithBoundaryCallable() {

        // TODO
//        public final <B> Observable<List<T>> buffer(Callable<? extends ObservableSource<B>> boundarySupplier)
    }

}
