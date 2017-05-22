package me.snippex.rxtest

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.TimeUnit

class ObservableCreationTest {

    @Test
    fun fromCallable() {

        //  [28:47.207] [main] first subscription
        //  [28:47.262] [main] inside fromCallable
        //  [28:48.262] [main] returning fromCallable
        //  [28:48.262] [main] 100
        //  [28:48.263] [main] second subscription
        //  [28:48.263] [main] inside fromCallable
        //  [28:49.264] [main] returning fromCallable
        //  [28:49.264] [main] 100

        val source1 = Observable.fromCallable {
            pp("inside fromCallable")
            Thread.sleep(1000)
            pp("returning fromCallable")
            100
        }

        pp("first subscription")
        source1.print()
        pp("second subscription")
        source1.print()

        val single = Single.fromCallable {
            Thread.sleep(1000)
            99
        }
        pp("first subscription of single")
        single.test().await().assertValue(99)
        pp("second subscription of single")
        single.test().await().assertValue(99)
    }

    @Test
    fun flowableWithBackpressure() {

        //  [00:56.229] [RxNewThreadScheduler-1] skipped 24 on 152 (before = 127)
        //  [00:56.239] [RxNewThreadScheduler-1] skipped 6416 on 6664 (before = 247)
        //  [00:56.240] [RxNewThreadScheduler-1] skipped 124 on 6884 (before = 6759)
        //  [00:56.240] [RxNewThreadScheduler-1] skipped 1154 on 8134 (before = 6979)
        //  [00:56.240] [RxNewThreadScheduler-1] skipped 130 on 8360 (before = 8229)
        //
        // 약간 랜덤하게 위에서처럼 나타날 것이다. onBackPressure 시리즈를 바꿔보는 것도 의미가 있다.
        // 또한 observeOn 을 빼면 어떤 현상이 발생하는지 체크해보는 것도 의미가 있다.
        // Flowable 과 Observable 의 차이도 살펴본다.

        var valueBefore: Long? = null
        var skippedCount = 0
        Flowable.interval(1, TimeUnit.MICROSECONDS)
//        Observable.interval(1, TimeUnit.MICROSECONDS)
                .onBackpressureDrop()
//                .doOnNext { pp("emitting(1) $it") }
                .observeOn(Schedulers.newThread())
//                .doOnNext { pp("emitting(2) $it") }
                .takeWhile { skippedCount < 5 }
                .subscribe {
                    if (valueBefore == null) {
                        valueBefore = it
                    } else if (it - valueBefore!! > 1) {
                        pp("skipped ${(it - valueBefore!!) - 1} on $it (before = $valueBefore)")
                        skippedCount++
                    }
                    valueBefore = it
                }

        Thread.sleep(3000)
    }

    @Test
    fun flowableCreation() {
        val stream = Flowable.create<Long>({ sub ->
            var position = 0L
            val disposable = Observable.interval(1, TimeUnit.MICROSECONDS)
                    .subscribe {
                        if (sub.requested() > 0) { // back-pressure aware
                            sub.onNext(position)
                            position++
                        }
                    }

            sub.setDisposable(disposable)
        }, BackpressureStrategy.ERROR)

        stream
                .observeOn(Schedulers.newThread())
                .subscribe { pp("$it") }

        Thread.sleep(5000)
    }

    @Test
    fun observableCreateVsDefer() {
        val createStream = Observable.create<Int> { sub ->
            sub.onNext(1)
            sub.onNext(2)
            sub.onNext(3)
            sub.onComplete()
        }

        // defer 를 사용하면 좀 더 편리할 수 있다!
        val deferStream = Observable.defer<Int> {
            Observable.just(1, 2, 3)
        }

        Observable.sequenceEqual(createStream, deferStream)
                .test().await().assertValue(true)
    }

}
