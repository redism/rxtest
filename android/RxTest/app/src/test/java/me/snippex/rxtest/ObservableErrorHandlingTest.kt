package me.snippex.rxtest

import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.rxkotlin.zipWith
import org.junit.Test
import java.util.concurrent.TimeUnit

class ObservableErrorHandlingTest {

    @Test
    fun onErrorReturnItem() {
        Observable.merge(
                generate("1,2,3", 100),
                Observable.error<Int>(IllegalArgumentException()).delay(150, TimeUnit.MILLISECONDS)
        )
                .onErrorReturnItem(999)
                .testResults(1, 999)

        Observable.merge(
                generate("1,2,3", 100),
                Observable.error<Int>(IllegalArgumentException()).delay(150, TimeUnit.MILLISECONDS)
                        .onErrorReturnItem(999)
        )
                .testResults(1, 999, 2, 3)

        // onErrorReturn 을 이용하여 throwable based 로 값을 반환할 수도 있음.
        // onExceptionResumeNext 도 있음.
    }

    @Test
    fun onErrorResumeNext() {
        Observable.merge(
                generate("1,2,3", 100),
                Observable.error<Int>(IllegalArgumentException()).delay(150, TimeUnit.MILLISECONDS)
                        .onErrorResumeNext(generate("10,20,30", 10))
        )
                .testResults(1, 10, 20, 30, 2, 3)

        //
        // Resuming based on exception.
        //
        val returnRecoverableStream = Function { ex: Throwable ->
            when (ex) {
                is IllegalArgumentException -> generate("1,2,3", 10)
                is IllegalStateException -> generate("-1,-2,-3", 10)
                else -> Observable.just(0)
            }
        }

        Observable.merge(
                generate("1,2,3", 100),
                Observable.error<Int>(IllegalArgumentException()).delay(150, TimeUnit.MILLISECONDS)
                        .onErrorResumeNext(returnRecoverableStream)
        )
                .testResults(1, 1, 2, 3, 2, 3)

        Observable.merge(
                generate("1,2,3", 100),
                Observable.error<Int>(IllegalStateException()).delay(150, TimeUnit.MILLISECONDS)
                        .onErrorResumeNext(returnRecoverableStream)
        )
                .testResults(1, -1, -2, -3, 2, 3)
    }

    @Test
    fun retry() {

        //  [16:40.624] [main] subscribing
        //  [16:40.638] [main] subscribe: 1
        //  [16:40.638] [main] subscribe: 2
        //  [16:40.638] [main] subscribe: 3
        //  [16:40.639] [main] Cancelling for retryCount 1
        //  [16:40.639] [main] subscribing
        //  [16:40.639] [main] disposing
        //  [16:40.640] [main] subscribe: 99
        //  [16:40.640] [main] subscribe: 100
        //  [16:40.640] [main] Cancelling for retryCount 2

        var retryCount = 0
        val observableSuccessOnRetry = Observable.create<Int> { sub ->
            retryCount++

            if (retryCount > 1) {
                sub.onNext(99)
                sub.onNext(100)
                sub.onComplete()
            } else {
                sub.onNext(1)
                sub.onNext(2)
                sub.onNext(3)
                sub.onError(IllegalStateException())
            }

            sub.setCancellable { pp("Cancelling for retryCount $retryCount") }
        }

        // retry 는 onError 호출시 수행되고, 이전 subscription 에 대해 dispose 가 되고 새롭게 subscription 이 발생하는 형태가 된다.
        observableSuccessOnRetry
                .doOnSubscribe { pp("subscribing") }
                .doOnDispose { pp("disposing") }
                .retry()
                .subscribe { pp("subscribe: $it") }

        Observable.timer(1, TimeUnit.SECONDS).test().await()
    }

    @Test
    fun retryWithPredicate() {
        val errorStream = Observable.just(1, 2, 3).concatWith(Observable.error(IllegalStateException()))

        errorStream
                .retry { retryCount: Int, exception: Throwable ->
                    pp("retryCount = $retryCount / exception = $exception")
                    retryCount < 3
                }
                .onErrorReturnItem(100)
                .testResults(1, 2, 3, 1, 2, 3, 1, 2, 3, 100)
    }

    @Test
    fun retryWithMaxCount() {
        val errorStream = Observable.just(1, 2, 3).concatWith(Observable.error(IllegalStateException()))

        errorStream.retry(2)
                .onErrorReturnItem(100)
                .testResults(1, 2, 3, 1, 2, 3, 1, 2, 3, 100)
    }

    @Test
    fun retryUntil() {
        val errorStream = Observable.just(1, 2, 3).concatWith(Observable.error(IllegalStateException()))

        var retryCount = 0
        errorStream
                .retryUntil {
                    retryCount++
                    retryCount > 2
                }
                .onErrorReturnItem(100)
                .testResults(1, 2, 3, 1, 2, 3, 1, 2, 3, 100)
    }

    @Test
    fun retryWhen() {

        //  [39:26.622] [main] retryWhen called
        //  [39:26.638] [main] subscribing..
        //  [39:26.640] [main] Source emitting error..
        //  [39:26.640] [main] Received 1 error - waiting 100 ms.
        //  [39:26.754] [RxComputationThreadPool-1] subscribing..
        //  [39:26.754] [RxComputationThreadPool-1] Source emitting error..
        //  [39:26.755] [RxComputationThreadPool-1] Received 2 error - waiting 200 ms.
        //  [39:26.960] [RxComputationThreadPool-2] subscribing..
        //  [39:26.960] [RxComputationThreadPool-2] Source emitting error..
        //  [39:26.960] [RxComputationThreadPool-2] Received 3 error - waiting 300 ms.
        //  [39:27.264] [RxComputationThreadPool-3] subscribing..
        //  [39:27.264] [RxComputationThreadPool-3] Source emitting error..
        //  [39:27.265] [RxComputationThreadPool-3] Received 4 error - will fail.

        val errorStream = Observable.just(1).concatWith(Observable.error<Int>(IllegalStateException()))
                .doOnSubscribe { pp("subscribing..") }
                .doOnError { pp("Source emitting error..") }

        // Error stream 을 우리가 원하는 형태로 제어 가능하다는 점에서 재미있는 것을 많이 할 수 있겠음.
        errorStream
                .retryWhen { attempts: Observable<Throwable> ->
                    pp("retryWhen called")
//                    attempts
//                            .subscribe {
//                                pp("attempts.subscribe : $it")
//                            }
//                    Observable.timer(3, TimeUnit.SECONDS)
//                    Observable.interval(3, TimeUnit.SECONDS)

                    // TODO: Zip with
                    attempts.zipWith(Observable.range(1, 4), { _: Throwable, i: Int -> i })
                            .flatMap {
                                if (it == 4) {
                                    pp("Received $it error - will fail.")
                                    Observable.error(IllegalArgumentException())
                                } else {
                                    pp("Received $it error - waiting ${it * 100} ms.")
                                    Observable.timer((it * 100).toLong(), TimeUnit.MILLISECONDS)
                                }
                            }
                }
                .onErrorReturnItem(100)
                .testResults(1, 1, 1, 1, 100)
    }

}

