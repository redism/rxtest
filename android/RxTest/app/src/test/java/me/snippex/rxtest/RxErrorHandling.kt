package me.snippex.rxtest

import io.reactivex.Observable
import org.junit.Test

class RxErrorHandling {

    @Test
    @Throws(Exception::class)
    fun observableFromThrowingInside() {
        Observable.fromCallable { throw IllegalStateException("Exception..") }
                .doOnEach { pp(" doOnEach : $it") }
                .subscribe({ pp(" subscribe.onNext : $it") },
                        { pp(" subscribe.onError : $it") })
    }

    @Test
    @Throws(Exception::class)
    fun observableFromNoErrorHandler() {
        //
        // 아래의 테스트는 단독으로 수행되어야 합니다.
        //
        Observable.fromCallable { throw IllegalStateException("Exception..") }
                .doOnEach { pp(" doOnEach : $it") }
                .subscribe({ pp(" subscribe.onNext : $it") })
    }

}

