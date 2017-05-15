package me.snippex.rxtest

import io.reactivex.Observable
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

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

}

