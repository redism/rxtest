package me.snippex.rxtest

import io.reactivex.Observable
import io.reactivex.functions.Function
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
}

