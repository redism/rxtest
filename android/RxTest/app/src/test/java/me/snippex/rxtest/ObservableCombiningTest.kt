package me.snippex.rxtest

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import org.junit.Test
import java.util.concurrent.TimeUnit

class ObservableCombiningTest {

//    @Test
//    fun andBasic() {
//        // TODO: and, then, when 은 기본 탑재 되어 있지 않다.
//    }
//
//    @Test
//    fun combineLatest() {
//        Observable.combineLatest<Int, Int, Int>(generate("1,2", 100), generate("4,5", 150),
//                BiFunction { e1, e2 -> e1 + e2 })
//                .testResults(5, 6, 7)
//    }
//
    @Test
    fun join() {
        //
        // 관전포인트
        // =======
        // http://www.introtorx.com/uat/content/v1.0.10621.0/17_SequencesOfCoincidence.html
        //
        //  [main] 0 - 10
        //  [main] 1 - 10
        //  [main] 2 - 10
        //  [main] 0 - 20
        //  [main] 1 - 20
        //  [main] 2 - 20
        //  [main] 3 - 10
        //  [main] 3 - 20
        //  [main] 4 - 10
        //  [main] 4 - 20
        //  [main] 0 - 30
        //  [main] 1 - 30
        //  [main] 2 - 30
        //  [main] 3 - 30
        //  [main] 4 - 30
        //  [main] 5 - 10
        //  [main] 5 - 20
        //  [main] 5 - 30

        generate("0,1,2,3,4,5", 50)
                .join<Int, Long, Long, String>(generate("10,20,30", 100),
                        Function { Observable.never() },
                        Function { Observable.never() },
                        BiFunction { x, y -> "$x - $y" })
                .blockingForEach { pp(it) }
//                .testResults("10 - 1", "40 - 2", "70 - 3")

//        generate("10,20,30", 300)
//                .join<Int, Long, Long, String>(generate("1,2,3,4,5,6,7,8", 100),
//                        Function { Observable.timer(it.toLong(), TimeUnit.MILLISECONDS) },
//                        Function { Observable.timer(it.toLong(), TimeUnit.MILLISECONDS) },
//                        BiFunction { x, y -> "$x - $y" })
////                .blockingForEach { pp(it) }
//                .testResults("10 - 1", "20 - 4", "30 - 7")
//
//        generate("10,20,30,40,50,60,70,80", 100)
//                .join<Int, Long, Long, String>(generate("1,2,3", 300),
//                        Function { Observable.timer(it.toLong(), TimeUnit.MILLISECONDS) },
//                        Function { Observable.timer(it.toLong(), TimeUnit.MILLISECONDS) },
//                        BiFunction { x, y -> "$x - $y" })
////                .blockingForEach { pp(it) }
//                .testResults("10 - 1", "40 - 2", "70 - 3")
    }

//    @Test
//    fun groupJoin() {
//        generate("10,20,30,40,50,60,70,80", 30)
//                .groupJoin<Int, Long, Long, Pair<Int, Observable<Int>>>(generate("1,2,3,4,5,6,7,8", 100),
//                        Function { Observable.timer(it.toLong(), TimeUnit.MILLISECONDS) },
//                        Function { Observable.timer(it.toLong(), TimeUnit.MILLISECONDS) },
//                        BiFunction { x, y -> Pair(x, y) })
//                .flatMap { (leftValue, rightValueObservable) ->
//                    rightValueObservable.map { "left = $leftValue, right = $it" }
//                            .doOnNext { pp("........... $it") }
//                }
//                .blockingForEach { pp(it) }
//    }
}

