package me.snippex.rxtest

import io.reactivex.Single
import org.junit.Test

class RxSingleTest {

    fun simplePositive(value: Int): Single<Int> {
        return Single.create { sub ->
            if (value > 0) {
                sub.onSuccess(value)
            } else {
                sub.onError(IllegalArgumentException())
            }
        }
    }

    @Test
    fun singleCreationSuccessScenario() {
        //
        // RESULTS
        // =======
        //  [main] Single.subscribe 10
        //
        simplePositive(10)
//                .doOnComplete { }         // Single 에는 doOnComplete 이 없다. success or error 이기 때문이다.
//                .doOnEach { }             // Single 에는 doOnEach 가 없다. 의미가 없기 때문이다.
                .subscribe { value ->
                    // kotlin 이 헷갈려 하기 때문에 반드시 argument 를 정의해주어야만 컴파일 가능
                    pp("Single.subscribe $value")
                }
    }

    @Test
    fun singleCreationErrorScenario() {
        //
        // RESULTS
        // =======
        //  [main] Single.subscribe.onError java.lang.IllegalArgumentException
        //
        simplePositive(-10)
                .subscribe({
                    pp("Single.subscribe $it")
                }, { error ->
                    pp("Single.subscribe.onError $error")
                })
    }

    @Test
    fun concatSingles() {

        // RESULTS
        // =======
        //  [main] Single.concat.doOnEach OnNextNotification[10]
        //  [main] Single.concat.subscribe 10
        //  [main] Single.concat.doOnEach OnNextNotification[20]
        //  [main] Single.concat.subscribe 20
        //  [main] Single.concat.doOnEach OnErrorNotification[java.lang.IllegalArgumentException]
        //  [main] Single.concat.subscribe 999
        //
        Single.concat(simplePositive(10), simplePositive(20), simplePositive(-10))
                .doOnEach { pp("Single.concat.doOnEach $it") }
                .onErrorReturn { 999 }
                .subscribe { pp("Single.concat.subscribe $it") }

        pp("======")

        Single.just(10).concatWith(Single.just(20))
                .subscribe { pp("Single.concatWith.subscribe $it") }

        pp("======")

        // TODO: concatWith 의 lambda 버전은 이상하게 제대로 동작하지 않는다.
//        val latch = CountDownLatch(1)
//        Single.just(10).concatWith {
//            pp("inside")
//            it.onSuccess(1)
//        }
//                .doOnEach { pp("Single.concat.doOnEach $it") }
//                .doOnComplete { latch.countDown() }
//                .subscribe { pp("Single.concat.subscribe $it") }
//
//        latch.await()
    }

    @Test
    fun flatMap() {

        //
        // RESULTS
        // =======
        //  [main] flatMap.subscribe 200
        //
        Single.just(10)
                .flatMap { input: Int -> Single.just(input * 20) }
                .subscribe { value -> pp("flatMap.subscribe $value") }
    }

}

