package me.snippex.rxtest

import io.reactivex.Observable
import org.junit.Test
import java.util.concurrent.TimeUnit

class ObservableGroupByTest {

//    @Test
//    fun basicGroupByUsage() {
//
//        //  [RxComputationThreadPool-1] Emitting 1 from source.
//        //  [RxComputationThreadPool-1] groupBy class => io.reactivex.internal.operators.observable.ObservableGroupBy$GroupedUnicast
//        //  [main] new grouped observable found! key = odd
//        //  [main] doOnEach from odd ==> OnNextNotification[1]
//        //  [RxComputationThreadPool-2] Emitting 3 from source.
//        //  [RxComputationThreadPool-2] doOnEach from odd ==> OnNextNotification[3]
//        //  [RxComputationThreadPool-3] Emitting 2 from source.
//        //  [RxComputationThreadPool-3] groupBy class => io.reactivex.internal.operators.observable.ObservableGroupBy$GroupedUnicast
//        //  [main] new grouped observable found! key = even
//        //  [main] doOnEach from even ==> OnNextNotification[2]
//        //  [RxComputationThreadPool-4] Emitting 4 from source.
//        //  [RxComputationThreadPool-4] doOnEach from even ==> OnNextNotification[4]
//        //  [RxComputationThreadPool-5] Emitting 5 from source.
//        //  [RxComputationThreadPool-5] groupBy class => io.reactivex.internal.operators.observable.ObservableGroupBy$GroupedUnicast
//        //  [main] new grouped observable found! key = odd
//        //  [main] doOnEach from odd ==> OnNextNotification[5]
//        //  [RxComputationThreadPool-5] doOnEach from odd ==> OnCompleteNotification
//        //
//        // 관전포인트
//        // =======
//        // 각 key 별로 새로운 Observable stream 이 언제 fork 되는지.
//        // 기존에 만들어졌던 observable stream 이 close 된 상태에서 해당 group 의 아이템이 발생하면 새롭게 생성됨.
//        // 주석에 있는 설명. 들어온 스트림을 사용하지 않을 때는 take(0) 를 해주라는 것
//        // delayError, bufferSize 가 있는 버전도 있음
//        //
//
//        generate("1,3,2,4,5", 50)
//                .doOnNext { pp("Emitting $it from source.") }
//                .groupBy({ value: Int -> if (value % 2 == 0) "even" else "odd" })
//                .doOnNext { pp("groupBy class => ${it.javaClass.name}") }
//                .blockingForEach { newSource: GroupedObservable<String, Int> ->
//                    pp("new grouped observable found! key = ${newSource.key}")
//                    newSource.doOnEach { pp("doOnEach from ${newSource.key} ==> $it") }
//                            .take(2)
//                            .subscribe()
//                }
//    }
//
//    @Test
//    fun groupByWithValueSelector() {
//
//        //  [main] doOnEach from odd ==> OnNextNotification[0]
//        //  [RxComputationThreadPool-2] doOnEach from odd ==> OnNextNotification[0]
//        //  [main] doOnEach from even ==> OnNextNotification[0]
//        //  [RxComputationThreadPool-4] doOnEach from even ==> OnNextNotification[0]
//        //  [main] doOnEach from odd ==> OnNextNotification[0]
//        //  [RxComputationThreadPool-5] doOnEach from odd ==> OnCompleteNotification
//        //
//        // 관전포인트
//        // =======
//        // valueSelector 를 추가하여 값을 한 번씩 변경해 줄 수 있음.
//
//        generate("1,3,2,4,5", 50)
//                .groupBy({ value: Int -> if (value % 2 == 0) "even" else "odd" },
//                        { _: Int -> 0 })
//                .blockingForEach { newSource: GroupedObservable<String, Int> ->
//                    newSource.doOnEach { pp("doOnEach from ${newSource.key} ==> $it") }
//                            .take(2)
//                            .subscribe()
//                }
//    }

    @Test
    fun groupJoin() {
        val leftStream = generate("1,2,3", 50)
                .doOnNext { pp("LeftStream.emitting item $it") }
        val rightStream = generate(",4,5,6", 50).map { it.toString() }
                .doOnNext { pp("RightStream.emitting item $it") }

        // TODO: Transformer with debug enabled

        leftStream.groupJoin<String, Int, Int, String>(rightStream,
                io.reactivex.functions.Function { value: Int ->
                    pp("leftEnd $value")
                    Observable.just(1).delay(10, TimeUnit.MILLISECONDS)
                            .doOnEach { pp("leftEnd.value($value).doOnEach : $it") }
//                    interval(0).doOnEach { pp("leftEnd.value($value).doOnEach : $it") }
                },
                io.reactivex.functions.Function { value: String ->
                    pp("rightEnd $value")
                    Observable.just(1).delay(10, TimeUnit.MILLISECONDS)
                            .doOnEach { pp("rightEnd.value($value).doOnEach : $it") }
//                    interval(0).doOnEach { pp("rightEnd.value($value).doOnEach : $it") }
                },
                io.reactivex.functions.BiFunction {
                    value1: Int, value2 ->
                    pp("resultSelector called with value $value1, $value2")

                    value2
                            .doOnSubscribe { pp("BiFunction.value2($value1).doOnSubscribe") }
                            .doOnEach { pp("BiFunction.value2($value1).doOnEach : $it") }
                            .doOnDispose { pp("BiFunction.value2($value1).doOnDispose") }
                            .subscribe()
                    String
                    "$$value1"
                })
                .blockingForEach {
                    pp("groupJoin.subscribe : $it")
                }
    }

}

