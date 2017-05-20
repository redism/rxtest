package me.snippex.rxtest

import io.reactivex.Observable
import io.reactivex.functions.Function
import org.junit.Test
import java.util.concurrent.TimeUnit

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

    }
}

