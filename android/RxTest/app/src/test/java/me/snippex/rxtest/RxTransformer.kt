package me.snippex.rxtest

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.CountDownLatch

class RxTransformer {

    class TestTransformer<T> : ObservableTransformer<T, T> {
        override fun apply(upstream: Observable<T>): ObservableSource<T> {
            return upstream.observeOn(Schedulers.newThread())
                    .subscribeOn(Schedulers.io())
        }
    }

    class MultiplierTransformer constructor(val multiplier: Int) : ObservableTransformer<Int, Int> {
        override fun apply(upstream: Observable<Int>): ObservableSource<Int> {
            return upstream.map { it * multiplier }
        }

    }

    class ToStringTransformer<T> : ObservableTransformer<T, String> {
        override fun apply(upstream: Observable<T>): ObservableSource<String> {
            return upstream.map { it.toString() }
        }
    }

    // 이처럼 정의하면 composable 한 유일한 transformer 가 정의된다.
    val singleInstanceTransformer = ObservableTransformer<Int, Int> { it.map { it + 1 } }

    @Test
    @Throws(Exception::class)
    fun simpleTransformer() {
        //
        // RESULTS
        // =======
        //  [main] waiting stream to be completed.
        //  [RxCachedThreadScheduler-1] createSimpleObservable, new subscriber found.
        //  [RxCachedThreadScheduler-1] first doOnEach : OnNextNotification[10]
        //  [RxCachedThreadScheduler-1] first doOnEach : OnCompleteNotification
        //  [RxNewThreadScheduler-1] second doOnEach : OnNextNotification[1001]
        //  [RxNewThreadScheduler-1] value subscribed => 1001
        //  [RxNewThreadScheduler-1] second doOnEach : OnCompleteNotification
        //
        // 관전 포인트
        // ========
        // Transformer 에 대한 이해
        // lambda 로 compose 를 사용할 때의 의미
        // flatMap 을 사용할 때와의 차이 : http://blog.danlew.net/2015/03/02/dont-break-the-chain/
        //

        fun createSimpleObservable(value: Int = 0): Observable<Int> {
            return Observable.create<Int> { sub ->
                pp("createSimpleObservable, new subscriber found.") // Scheduler => Schedulers.newThread()
                sub.onNext(value)
                sub.onComplete()
            }
        }

        val latch = CountDownLatch(1)
        val disposeBag = CompositeDisposable()
        createSimpleObservable(10)
                .doOnEach { pp("first doOnEach : $it") }
                .compose(TestTransformer())
                .compose(singleInstanceTransformer)
                .compose(MultiplierTransformer(100))
                .compose { it.map { it + 1 } }
                .compose(ToStringTransformer())
                .doOnEach { pp("second doOnEach : $it") }
                .doOnComplete { latch.countDown() }
                .subscribe { pp("value subscribed => $it") }
                .addTo(disposeBag)

        pp("waiting stream to be completed.")
        latch.await()
        disposeBag.dispose()
    }

}
