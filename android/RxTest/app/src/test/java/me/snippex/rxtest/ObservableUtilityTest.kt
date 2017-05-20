package me.snippex.rxtest

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Timed
import io.reactivex.subjects.BehaviorSubject
import org.junit.Test
import java.util.concurrent.TimeUnit

class DebugObserver<T> : Observer<T> {
    override fun onSubscribe(d: Disposable) {
    }

    override fun onError(e: Throwable?) {
    }

    override fun onNext(t: T) {
    }

    override fun onComplete() {
    }

    fun hey() {
        pp("Hey, it's me!")
    }
}

class ObservableUtilityTest {

    @Test
    fun delay() {
        generate("1,2,3", 10)
                .delay(100, TimeUnit.MILLISECONDS)
                .testResults(1, 2, 3)

        // delaying per-item basis
        generate("1,2,3", 10)
                .delay { value ->
                    pp("Generating delay observable for : $value")
                    Observable.timer((value * 100).toLong(), TimeUnit.MILLISECONDS)
                }
                .testResults(1, 2, 3)

        // delayError, scheduler

        // delaySubscription!!
        generate("1,2,3", 10)
                .printEach()
                .delaySubscription(1000, TimeUnit.MILLISECONDS)
                .print()

        // delaying subscription and each item.
        generate("1,2,3", 10)
                .delay<Long, Long>(Observable.timer(3000, TimeUnit.MILLISECONDS),
                        Function { value -> Observable.timer((value * 1000).toLong(), TimeUnit.MILLISECONDS) })
                .print()

        // TODO: doOnSubscribe 구현이 어떤지 몰라도 delaySubscription 을 했을 때 약간 묘하게 동작하는 부분이 있음.
        pp("======")
        generate("1,2,3", 1000)
                .doOnSubscribe { pp("doOnSubscribe 1") }
                .delaySubscription(1, TimeUnit.SECONDS)
                .doOnSubscribe { pp("doOnSubscribe 2") }
                .subscribe { pp("$it") }

        Observable.timer(4, TimeUnit.SECONDS).testResults(0)
    }

    @Test
    fun doFunction() {

        //  [main] doOnSubscribe, io.reactivex.internal.operators.observable.ObservableDoOnEach$DoOnEachObserver@335eadca
        //  [RxComputationThreadPool-1] doOnNext, 1
        //  [RxComputationThreadPool-1] doOnEach, OnNextNotification[1]
        //  [RxComputationThreadPool-1] subscribe 1
        //  [RxComputationThreadPool-1] doAfterNext 1
        //  [RxComputationThreadPool-2] doOnNext, 2
        //  [RxComputationThreadPool-2] doOnEach, OnNextNotification[2]
        //  [RxComputationThreadPool-2] subscribe 2
        //  [RxComputationThreadPool-2] doAfterNext 2
        //  [RxComputationThreadPool-3] doOnNext, 3
        //  [RxComputationThreadPool-3] doOnEach, OnNextNotification[3]
        //  [RxComputationThreadPool-3] subscribe 3
        //  [RxComputationThreadPool-3] doAfterNext 3
        //  [RxComputationThreadPool-3] doOnEach, OnCompleteNotification
        //  [RxComputationThreadPool-3] doOnComplete
        //  [RxComputationThreadPool-3] doOnTerminate
        //  [RxComputationThreadPool-3] doFinally
        //  [RxComputationThreadPool-3] doAfterTerminate

        generate("1,2,3", 10)
                .doAfterNext { pp("doAfterNext $it") }
                .doOnNext { pp("doOnNext, $it") }
                .doOnSubscribe { pp("doOnSubscribe, $it") }
                .doOnDispose { pp("doOnDispose") }
                .doOnEach { pp("doOnEach, $it") }
                .doOnComplete { pp("doOnComplete") }
                .doOnError { pp("doOnError") }
                .doAfterTerminate { pp("doAfterTerminate") }
                .doOnTerminate { pp("doOnTerminate") } // doOnComplete or doOnError
                .doFinally { pp("doFinally") } // terminates or cancelled
                .subscribe { pp("subscribe $it") }

        // TODO: Action, observer, consumer

        // Holding test case thread.
        Observable.timer(1, TimeUnit.SECONDS).testResults(0)
    }

    @Test
    fun materialize_dematerialize() {
        //  [main] emitting 1
        //  [main] materialized OnNextNotification[1]
        //  [main] de-materialized 1
        //  [main] emitting 2
        //  [main] materialized OnNextNotification[2]
        //  [main] de-materialized 2
        //  [main] emitting 3
        //  [main] materialized OnNextNotification[3]
        //  [main] de-materialized 3
        //  [main] materialized OnCompleteNotification

        Observable.range(1, 3)
                .printNext("emitting ")
                .materialize()
                .printNext("materialized ")
                .dematerialize<Int>()
                .printNext("de-materialized ")
                .testResults(1, 2, 3)
    }

    @Test
    fun serialize() {
        // http://stackoverflow.com/a/43148352/388351
    }

    @Test
    fun subscribeWith() {
        // observer 를 반환한다. chaining 을 끊기지 않게 하기 위한 다양한 방법이 있구나.
        generate("1,2,3", 10)
                .subscribeWith(DebugObserver<Int>())
                .hey()
    }

    @Test
    fun intervalRange() {
        // start, count, initial delay, item delay
        Observable.intervalRange(1, 3, 1000, 1000, TimeUnit.MILLISECONDS)
                .delaySubscription(1, TimeUnit.SECONDS)
                .print()
    }

    @Test
    fun timeout() {
        generate("1,2,3", 500)
                .timeout(200, TimeUnit.MILLISECONDS)
                .onErrorReturnItem(100)
                .testResults(1, 100)

        generate("7,4,1", 500)
                .delay(1, TimeUnit.SECONDS) // first item never timed out
                .timeout { Observable.timer((it * 100).toLong(), TimeUnit.MILLISECONDS) }
                .onErrorReturnItem(100)
                .testResults(7, 4, 100)
    }

    @Test
    fun timeoutWithFallback() {
        //  [27:01.877] [main] 7
        //  [27:02.381] [main] 4
        //  [27:02.787] [main] 1
        //  [27:03.792] [main] 2
        //  [27:04.792] [main] 3

        generate("7,4,1", 500)
                .delay(1, TimeUnit.SECONDS) // first item never timed out
                .timeout<Long>(Function { Observable.timer((it * 100).toLong(), TimeUnit.MILLISECONDS) },
                        generate("1,2,3", 1000))
                .testResults(7, 4, 1, 2, 3)

        val subject = BehaviorSubject.createDefault(-100)
        subject.onNext(50)
        generate("7,4,1", 500)
                .delay(1, TimeUnit.SECONDS) // first item never timed out
                .timeout<Long>(Function { Observable.timer((it * 100).toLong(), TimeUnit.MILLISECONDS) },
                        subject.take(1))
                .testResults(7, 4, 50)

        // Scheduler, fallback observable, first item timeout indicator, item timeout indicator etc.
    }

    @Test
    fun timestamp() {
        generate("1,2,3", 100)
                .timestamp(TimeUnit.MILLISECONDS)
                .scan { t1: Timed<Int>, t2: Timed<Int> ->
                    pp("Item (${t2.value()}) emitted after ${t2.time() - t1.time()} milliseconds, prev value = ${t1.value()}")
                    t2
                }
                .blockingSubscribe()
    }

    @Test
    fun using() {

        //  [47:43.436] [main] FakeDBConnection allocated.
        //  [47:43.514] [main] 1
        //  [47:43.618] [main] 2
        //  [47:43.718] [main] 3
        //  [47:43.718] [RxComputationThreadPool-3] FakeDBConnection de-allocated.

        class FakeDBConnection {
            init {
                pp("FakeDBConnection allocated.")
            }

            fun genStream(): Observable<Int> {
                return generate("1,2,3", 100)
            }

            fun close() {
                pp("FakeDBConnection de-allocated.")
            }
        }

        Observable.using({ FakeDBConnection() }, // resource supplier
                { db -> db.genStream() },
                { db -> db.close() })
                .print()
    }
}

