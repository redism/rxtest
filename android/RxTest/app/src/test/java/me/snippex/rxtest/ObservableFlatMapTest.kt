package me.snippex.rxtest

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import java.util.concurrent.TimeUnit

class ObservableFlatMapTest {

    @Test
    fun flatMapBasic() {
        //  [main] subscribe : 1
        //  [main] subscribe : 2
        //  [main] subscribe : 2
        //  [main] subscribe : 3
        //  [main] subscribe : 3
        //  [main] subscribe : 3

        generate("1,2,3")
                .flatMap { Observable.just(it).repeat(it.toLong()) }
                .blockingForEach { pp("subscribe : $it") }
    }

    @Test
    fun flatMapOverlapping() {

        //  [RxComputationThreadPool-1] [1] 1
        //  [RxComputationThreadPool-4] [3] (1) => OnNextNotification[1]
        //  [RxComputationThreadPool-4] [3] (1) => OnCompleteNotification
        //  [main] [6] subscribe : 1
        //  [RxComputationThreadPool-2] [1] 2
        //  [RxComputationThreadPool-1] [3] (2) => OnNextNotification[2]
        //  [main] [6] subscribe : 2
        //  [RxComputationThreadPool-3] [1] 3
        //  [RxComputationThreadPool-3] [2] Main stream completed, upper stream.
        //  [RxComputationThreadPool-1] [3] (2) => OnNextNotification[2]
        //  [RxComputationThreadPool-1] [3] (2) => OnCompleteNotification
        //  [main] [6] subscribe : 2
        //  [RxComputationThreadPool-2] [3] (3) => OnNextNotification[3]
        //  [main] [6] subscribe : 3
        //  [RxComputationThreadPool-2] [3] (3) => OnNextNotification[3]
        //  [main] [6] subscribe : 3
        //  [RxComputationThreadPool-2] [3] (3) => OnNextNotification[3]
        //  [RxComputationThreadPool-2] [3] (3) => OnCompleteNotification
        //  [main] [6] subscribe : 3
        //  [RxComputationThreadPool-2] [5] Main stream completed.
        //
        // 관전포인트
        // =======
        // flatMap 이 하류로 어떻게 가는가?
        // flatMap 과 switchMap 은 어떻게 다른가?
        // upper stream 에서의 completion 과 flatMap 이후의 completion 이 다르다는 것.

        generate("1,2,3", 500)
                .doOnNext { pp("[1] $it") }
                .doOnComplete { pp("[2] Main stream completed, upper stream.") }
                .flatMap { flatMapInput ->
                    Observable.interval(300, TimeUnit.MILLISECONDS)
                            .map { flatMapInput }
                            .take(flatMapInput.toLong())
                            .doOnEach { pp("[3] ($flatMapInput) => $it") }
                            .doOnDispose { pp("[4] Internal flatMap disposed.") }
                }
                .doOnComplete { pp("[5] Main stream completed.") }
                .blockingForEach { pp("[6] subscribe : $it") }
    }

    @Test
    fun flatMapDelayError() {

        //  [RxComputationThreadPool-1] doOnEach : OnErrorNotification[java.lang.IllegalArgumentException]
        //  [main] subscribe : 777
        //  [main] ========
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[2]
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[2]
        //  [RxComputationThreadPool-1] doOnEach : OnNextNotification[2]
        //  [main] subscribe : 2
        //  [main] subscribe : 2
        //  [main] subscribe : 2
        //  [RxComputationThreadPool-2] doOnEach : OnNextNotification[3]
        //  [RxComputationThreadPool-2] doOnEach : OnNextNotification[3]
        //  [main] subscribe : 3
        //  [RxComputationThreadPool-2] doOnEach : OnNextNotification[3]
        //  [main] subscribe : 3
        //  [main] subscribe : 3
        //  [RxComputationThreadPool-2] doOnEach : OnErrorNotification[java.lang.IllegalArgumentException]
        //  [main] subscribe : 777
        //
        // 관전포인트
        // =======
        // delayError 를 했을 때와 하지 않았을 때의 차이점 체크

        generate("1,2,3")
                .flatMap({
                    if (it == 1) {
                        Observable.error(IllegalArgumentException())
                    } else {
                        Observable.just(it).repeat(3)
                    }
                }, false)
                .doOnEach { pp("doOnEach : $it") }
                .onErrorReturnItem(777)
                .blockingForEach { pp("subscribe : $it") }

        pp("========")

        generate("1,2,3")
                .flatMap({
                    if (it == 1) {
                        Observable.error(IllegalArgumentException())
                    } else {
                        Observable.just(it).repeat(3)
                    }
                }, true)
                .doOnEach { pp("doOnEach : $it") }
                .onErrorReturnItem(777)
                .blockingForEach { pp("subscribe : $it") }
    }

    @Test
    fun flatMapComplex() {

        //  [main] subscribe (concurrency = 1) : 1
        //  [main] subscribe (concurrency = 1) : 1
        //  [main] subscribe (concurrency = 1) : 1
        //  [main] subscribe (concurrency = 1) : 2
        //  [main] subscribe (concurrency = 1) : 2
        //  [main] subscribe (concurrency = 1) : 2
        //  [main] subscribe (concurrency = 1) : 3
        //  [main] subscribe (concurrency = 1) : 3
        //  [main] subscribe (concurrency = 1) : 3
        // ===== [main] subscribe (concurrency = 2) : 2
        //  [main] subscribe (concurrency = 2) : 1
        //  [main] subscribe (concurrency = 2) : 2
        //  [main] subscribe (concurrency = 2) : 1
        //  [main] subscribe (concurrency = 2) : 1
        //  [main] subscribe (concurrency = 2) : 2
        //  [main] subscribe (concurrency = 2) : 3
        //  [main] subscribe (concurrency = 2) : 3
        //  [main] subscribe (concurrency = 2) : 3
        //
        // 관전포인트
        // =======
        // concurrency 제어가 얼마나 쉽게요?

        generate("1,2,3", 0)
                .flatMap({ v ->
                    Observable.interval(100, TimeUnit.MILLISECONDS).map { v }.take(3)
                }, false, 1, 100)
                .blockingForEach { pp("subscribe (concurrency = 1) : $it") }

        print("=====")

        generate("1,2,3", 0)
                .flatMap({ v ->
                    Observable.interval(100, TimeUnit.MILLISECONDS).map { v }.take(3)
                }, false, 2, 100)
                .blockingForEach { pp("subscribe (concurrency = 2) : $it") }
    }

    @Test
    fun flatMapBufferSize() {
        // bufferSize 는 inner ObservableSource 에서 prefetch 할 갯수라는데, 정확하게 이해가 가지 않는다.
        // 이건 잘 모르겠다.

        generate("1,2,3,4,5", 0)
                .doOnEach { pp("doOnEach : $it") }
                .flatMap({ v ->
                    Observable.interval(100, TimeUnit.MILLISECONDS).map { v }.take(10)
                }, false, 2, 10)
                .blockingForEach { pp("subscribe (concurrency = 2) : $it") }
    }

    @Test
    fun flatMapDetailed() {

        //  [main] subscribe : 1
        //  [main] subscribe : 2
        //  [RxComputationThreadPool-3] passing completion
        //  [main] subscribe : 3
        //  [main] subscribe : 999
        //  [main] ===========
        //  [main] subscribe : 1
        //  [RxComputationThreadPool-1] passing error
        //  [main] subscribe : 777
        //
        // 관전포인트
        // =======
        // onError, onCompletion 중에 하나만 불린다는 것을 확인해보자.

        generate("1,2,3", 100)
                .doOnComplete { pp("passing completion") }
                .doOnError { pp("passing error") }
                .flatMap({ Observable.just(it) }, // onNext
                        { Observable.just(777) }, // onError
                        { Observable.just(999) }) // onCompletion
                .blockingForEach { pp("subscribe : $it") }

        pp("===========")

        generate("1,2,3", 100)
                .doOnNext {
                    if (it == 2) {
                        throw IllegalStateException()
                    }
                }
                .doOnComplete { pp("passing completion") }
                .doOnError { pp("passing error") }
                .flatMap({ Observable.just(it) }, // onNext
                        { Observable.just(777) }, // onError
                        { Observable.just(999) }) // onCompletion
                .blockingForEach { pp("subscribe : $it") }
    }

    @Test
    fun flatMapResultSelector() {

        //  [RxComputationThreadPool-4] resultSelector => 1, 10
        //  [main] subscribe : 1 => 10
        //  [RxComputationThreadPool-4] resultSelector => 1, 10
        //  [main] subscribe : 1 => 10
        //  [RxComputationThreadPool-1] resultSelector => 2, 20
        //  [main] subscribe : 2 => 20
        //  [RxComputationThreadPool-4] resultSelector => 1, 10
        //  [main] subscribe : 1 => 10
        //  [RxComputationThreadPool-1] resultSelector => 2, 20
        //  [main] subscribe : 2 => 20
        //  [RxComputationThreadPool-2] resultSelector => 3, 30
        //  [main] subscribe : 3 => 30
        //  [RxComputationThreadPool-1] resultSelector => 2, 20
        //  [main] subscribe : 2 => 20
        //  [RxComputationThreadPool-2] resultSelector => 3, 30
        //  [main] subscribe : 3 => 30
        //  [RxComputationThreadPool-2] resultSelector => 3, 30
        //  [main] subscribe : 3 => 30
        //
        // 관전포인트
        // =======
        // ResultSelector 를 이용하여 오리지널 데이터를 함께 땡겨올 수 있다는 것. 잘만 쓴다면 매우 편리할 수 있겠음.
        // 예를 들어 input data 를 이용해서 서버 API 호출을 수행한다음에 input data 를 따로 저장하지 않고도 넘겨줄 수 있겠음.

        generate("1,2,3", 100)
                .flatMap({ value -> Observable.interval(100, TimeUnit.MILLISECONDS).map { value * 10 }.take(3) },
                        { value1, value2 ->
                            pp("resultSelector => $value1, $value2")
                            "$value1 => $value2"
                        })
                .blockingForEach { pp("subscribe : $it") }
    }

    @Test
    fun flatMapCompletable() {

        //  [RxComputationThreadPool-1] Value 1 mapped to completable.
        //  [RxComputationThreadPool-2] Value 2 mapped to completable.
        //  [RxComputationThreadPool-3] Value 3 mapped to completable.
        //  [main] flatMapCompletable completed.
        //
        // 관전포인트
        // =======
        // delayError 옵션이 있는 버전도 있으니 참고.

        generate("1,2,3", 100)
                .flatMapCompletable {
                    pp("Value $it mapped to completable.")
                    Completable.complete()
                }
                .blockingGet()

        pp("flatMapCompletable completed.")
    }

    @Test
    fun flatMapIterable() {

        //  [main] flatMapIterable.subscribe : 1
        //  [main] flatMapIterable.subscribe : 1
        //  [main] flatMapIterable.subscribe : 1
        // << 1000ms delay >>
        //  [main] flatMapIterable.subscribe : 2
        //  [main] flatMapIterable.subscribe : 2
        //  [main] flatMapIterable.subscribe : 2
        // << 1000ms delay >>
        //  [main] flatMapIterable.subscribe : 3
        //  [main] flatMapIterable.subscribe : 3
        //  [main] flatMapIterable.subscribe : 3

        generate("1,2,3", 1000)
                .flatMapIterable { arrayListOf(it, it, it) }
                .blockingForEach { pp("flatMapIterable.subscribe : $it") }
    }

    @Test
    fun flatMapIterableWithResultSelector() {

        //  [main] flatMapIterableWithResultSelector.subscribe : 2
        //  [main] flatMapIterableWithResultSelector.subscribe : 2
        //  [main] flatMapIterableWithResultSelector.subscribe : 2
        // << 1000ms delay >>
        //  [main] flatMapIterableWithResultSelector.subscribe : 4
        //  [main] flatMapIterableWithResultSelector.subscribe : 4
        //  [main] flatMapIterableWithResultSelector.subscribe : 4
        // << 1000ms delay >>
        //  [main] flatMapIterableWithResultSelector.subscribe : 6
        //  [main] flatMapIterableWithResultSelector.subscribe : 6
        //  [main] flatMapIterableWithResultSelector.subscribe : 6

        generate("1,2,3", 1000)
                .flatMapIterable({ arrayListOf(it, it, it) },
                        { value1, value2 -> value1 + value2 })
                .blockingForEach { pp("flatMapIterableWithResultSelector.subscribe : $it") }
    }

    @Test
    fun flatMapMaybe() {
        // TODO: After MaybeSource
//        generate("1,2,3", 1000)
//                .flatMapMaybe {  }
    }

    @Test
    fun flatMapSingle() {

        //  [main] flatMapSingle.subscribe : 10
        //  [main] flatMapSingle.subscribe : 20
        //  [main] flatMapSingle.subscribe : 30

        generate("1,2,3", 500)
                .flatMapSingle { Single.just(it * 10) }
                .blockingForEach { pp("flatMapSingle.subscribe : $it") }
    }

}

