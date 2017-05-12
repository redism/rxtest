package me.snippex.rxtest

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class ObservableBufferTest {

    @Test
    fun testGenerateObservable() {
        generate("1,2,3,,,,4,5")
                .blockingForEach { pp("subscribe $it") }
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

        //  [main] Callable called.
        //  [RxComputationThreadPool-2] doOnEach : OnNextNotification[1]
        //  [RxComputationThreadPool-3] doOnEach : OnNextNotification[2]
        //  [RxComputationThreadPool-4] doOnEach : OnNextNotification[3]
        //  [RxComputationThreadPool-1] Callable disposed.
        //  [RxComputationThreadPool-1] Callable called.
        //  [main] bufferWithCustomOpeningAndClosingNotClosing.subscribe : [1, 2, 3]
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[4]
        //  [RxComputationThreadPool-2] doOnEach : OnNextNotification[5]
        //  [RxComputationThreadPool-2] doOnEach : OnCompleteNotification
        //  [RxComputationThreadPool-2] Callable disposed.
        //  [main] bufferWithCustomOpeningAndClosingNotClosing.subscribe : [4, 5]
        //
        // 관전포인트
        // =======
        // Callable 이 아닌 Observable 버전과의 차이점. 이 Callable 의 경우에는 매 boundary 마다 새로운 boundarySupplier 를 받는다는 차이점이 있다.

//        public final <B> Observable<List<T>> buffer(Callable<? extends ObservableSource<B>> boundarySupplier)
        generate("1,2,3,4,5", 100)
                .doOnEach { pp("doOnEach : $it") }
                .buffer(Callable<Observable<Long>> {
                    pp("Callable called.")
                    Observable.interval(300, TimeUnit.MILLISECONDS)
                            .doOnDispose { pp("Callable disposed.") }
                })
                .blockingForEach { pp("bufferWithCustomOpeningAndClosingNotClosing.subscribe : $it") }
    }

}
