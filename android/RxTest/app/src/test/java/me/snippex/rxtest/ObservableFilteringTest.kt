package me.snippex.rxtest

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.TreeSet
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException

class ObservableFilteringTest {

    @Test
    fun debounce() {

        //  [RxComputationThreadPool-1] doOnNext : 5

        generate("1,2,3,4,5", 250)
                .debounce(300, TimeUnit.MILLISECONDS)
                .doOnNext { pp("doOnNext : $it") }
                .test()
                .await()
                .assertValues(5)
    }

    @Test
    fun debounceWithSelector() {

        //  [RxComputationThreadPool-1] insideDebounce : 1
        //  [RxComputationThreadPool-1] doOnNext : 1
        //  [RxComputationThreadPool-2] insideDebounce : 2
        //  [RxComputationThreadPool-3] doOnNext : 2
        //  [RxComputationThreadPool-3] insideDebounce : 3
        //  [RxComputationThreadPool-4] insideDebounce : 4
        //  [RxComputationThreadPool-2] insideDebounce : 5
        //  [RxComputationThreadPool-2] doOnNext : 5
        //
        // 스케줄러가 있는 버전도 있음.

        generate("1,2,3,4,5", 250)
                .debounce {
                    pp("insideDebounce : $it")
                    Observable.timer((it * 100).toLong(), TimeUnit.MILLISECONDS)
                }
                .doOnNext { pp("doOnNext : $it") }
                .test()
                .await()
                .assertValues(1, 2, 5)
    }

    @Test
    fun distinct() {

        //  [RxComputationThreadPool-3] doOnNext : 1
        //  [RxComputationThreadPool-4] doOnNext : 2
        //  [RxComputationThreadPool-1] doOnNext : 3
        //  [RxComputationThreadPool-3] doOnNext : 4

        generate("1,2,3,1,4,3", 100)
                .distinct()
                .doOnNext { pp("doOnNext : $it") }
                .test()
                .await()
                .assertValues(1, 2, 3, 4)
    }

    @Test
    fun distinctWithComplexArguments() {
        generate("1,2,3,1,4,3", 100)
                .distinct({ it <= 3 }, // keySelector
                        { TreeSet<Boolean>() }) // collectionSupplier. (default = HashSet)
                .test()
                .await()
                .assertValues(1, 4)
    }

    @Test
    fun elementAt() {
        generate("1,2,3", 100)
                .elementAt(2)
                .test()
                .await()
                .assertResult(3)

        generate("1,2,3", 100)
                .elementAt(4)
                .test()
                .await()
                .assertValueCount(0)
                .assertComplete()

        generate("1,2,3", 100)
                .elementAt(4, 100)
                .test()
                .await()
                .assertResult(100)

        generate("1,2,3", 100)
                .elementAtOrError(4)
                .test()
                .await()
                .assertError(NoSuchElementException::class.java)
    }

    @Test
    fun filter() {
        generate("1,2,3", 100)
                .filter { it > 2 }
                .test().await().assertResult(3)
    }

    @Test
    fun first() {
        generate("1,2,3", 100)
                .firstElement()
                .test().await().assertResult(1)

        generate("", 100)
                .firstElement()
                .test().await().assertValueCount(0).assertComplete()

        generate("", 100)
                .first(999)
                .test().await().assertResult(999)

        generate("", 100)
                .firstOrError()
                .test().await().assertFailure(NoSuchElementException::class.java)
    }

    @Test
    fun ignoreElements() {
        generate("1,2,3", 100)
                .ignoreElements()
                .test().await().assertResult()
    }

    @Test
    fun isEmpty() {
        generate("1,2,3", 100)
                .filter { it > 3 }
                .isEmpty
                .test().await().assertResult(true)

        generate("1,2,3", 100)
                .filter { it > 2 }
                .isEmpty
                .test().await().assertResult(false)
    }

    @Test
    fun last() {
        generate("1,2,3", 100)
                .last(99)
                .test().await().assertResult(3)

        generate("", 100)
                .last(99)
                .test().await().assertResult(99)

        generate("", 100)
                .lastOrError()
                .test().await().assertFailure(NoSuchElementException::class.java)
    }

    @Test
    fun sample() {
        generate("1,2,3,4,5", 120)
                .sample(250, TimeUnit.MILLISECONDS)
                .test().await().assertResult(3)

        generate("1,2,3,4,5", 120)
                .sample(250, TimeUnit.MILLISECONDS, true)
                .test().await().assertResult(3, 5)

        generate("1,2,3,4,5,6", 120)
                .sample(250, TimeUnit.MILLISECONDS, Schedulers.newThread(), false)
                .test().await().assertResult(3, 5)

        generate("1,2,3,4,5,6", 120)
                .sample(Observable.interval(250, TimeUnit.MILLISECONDS), true)
                .test().await().assertResult(3, 5, 6)
    }

    @Test
    fun skip() {
        generate("1,2,3", 100)
                .skip(1)
                .test().await().assertResult(2, 3)

        generate("1,2,3,4,5", 100)
                .skip(250, TimeUnit.MILLISECONDS)
                .test().await().assertResult(4, 5)

        generate("1,2,3,4", 200)
                .skip(500, TimeUnit.MILLISECONDS, Schedulers.newThread())
                .test().await().assertResult(4)
    }

    @Test
    fun skipLast() {
        generate("1,2,3", 100)
                .skipLast(1)
                .test().await().assertResult(1, 2)

        generate("1,2,3", 100)
                .skipLast(150, TimeUnit.MILLISECONDS, Schedulers.newThread(), true, 100)
                .test().await().assertResult(1)
    }

    @Test
    fun take() {
        generate("1,2,3", 100)
                .take(1)
                .testResults(1)

        generate("1,2,3", 100)
                .take(150, TimeUnit.MILLISECONDS)
                .testResults(1, 2)
    }

    @Test
    fun takeLast() {
        generate("1,2,3,4,5", 100)
                .takeLast(1, 200, TimeUnit.MILLISECONDS, Schedulers.newThread(), true, 100)
                .testResults(5)
    }

    @Test
    fun ofType() {

        //  [12:33.978] [main] before ofType : 1
        //  [12:33.986] [main] after ofType : 1
        //  [12:33.986] [main] before ofType : 2
        //  [12:33.986] [main] after ofType : 2
        //  [12:33.987] [main] before ofType : 3
        //  [12:33.987] [main] before ofType : 4
        //  [12:33.987] [main] after ofType : 4

        Observable.just(1, 2, "3", 4)
                .printNext("before ofType : ")
                .ofType(Integer::class.java)
                .printNext("after ofType : ")
                .map { i -> i.toInt() }
                .testResults(1, 2, 4)
    }
}

