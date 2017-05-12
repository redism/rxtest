package me.snippex.rxtest

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch

class RxSimpleTest {

    @Test
    @Throws(Exception::class)
    fun rxToBlockingShouldWork() {
        assertEquals(Observable.just(1).blockingFirst(), 1)
    }

    @Test
    fun addToAndCompositeDisposable() {
        val disposeBag = CompositeDisposable()

        Observable.just(10)
                .subscribe()
                .addTo(disposeBag)          // TODO: addTo has been added?
    }

    @Test
    @Throws(Exception::class)
    fun rxObserveOnVsSubscribeOn() {
        //
        // observeOn 과 subscribeOn 과의 차이점을 설명합니다. 아래의 결과에서 볼 수 있는 것처럼 subscribeOn은 subscription 이 발생하는
        // 스케줄러를 결정하고 observeOn은 이후의 스트림을 어떤 스케줄러에서 처리할 것인지를 결정한다.
        //
        // RESULTS
        // =======
        //  [main]  waiting stream to be completed.
        //  [RxNewThreadScheduler-1]  createSimpleObservable, new subscriber found.
        //  [RxNewThreadScheduler-1]  first doOnEach : OnNextNotification[10]
        //  [RxNewThreadScheduler-1]  first doOnEach : OnCompleteNotification
        //  [RxComputationThreadPool-1]  second doOnEach : OnNextNotification[10]
        //  [RxComputationThreadPool-1]  value subscribed => 10
        //  [RxComputationThreadPool-1]  second doOnEach : OnCompleteNotification
        //
        // 관전 포인트
        // ========
        // 각 출력이 발생한 쓰레드
        // 각 출력이 발생한 순서 (실행할 때마다 순서가 바뀌기도 함)
        //

        fun createSimpleObservable(value: Int = 0): Observable<Int> {
            return Observable.create<Int> { sub ->
                pp(" createSimpleObservable, new subscriber found.") // Scheduler => Schedulers.newThread()

                sub.onNext(value)
                sub.onComplete()
            }
        }

        val latch = CountDownLatch(1)
        val disposeBag = CompositeDisposable()
        createSimpleObservable(10)
                .doOnEach { pp(" first doOnEach : $it") }
                .subscribeOn(Schedulers.newThread())
                .subscribeOn(Schedulers.io())               // subscribeOn 이 여러번 불려도 가장 앞의 것이 선택된다.
                .observeOn(Schedulers.computation())
                .doOnEach { pp(" second doOnEach : $it") }
                .doOnComplete { latch.countDown() }
                .subscribe { pp(" value subscribed => $it") }
                .addTo(disposeBag)

        pp(" waiting stream to be completed.")
        latch.await() // 테스트는 main thread 를 기준으로 동작하므로 락을 걸지 않으면 파이프라인이 종료되기 전에 테스트가 끝나버린다.
        disposeBag.dispose()
    }


    @Test
    @Throws(Exception::class)
    fun observableFrom() {
        Observable.fromCallable { 100 }
                .doOnEach { pp(" $it") }
                .subscribe { pp(" $it") }
    }

    // http://reactivex.io/documentation/ko/single.html
    @Test
    fun rxSingle() {
//        Single.just(10).compose { upstream: Single<Int> ->
//
//        }
    }


    fun differenceBetweenObserveOnAndSubcribeOn() {

    }

}