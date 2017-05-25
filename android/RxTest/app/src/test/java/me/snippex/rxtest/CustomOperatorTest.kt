package me.snippex.rxtest

import io.reactivex.*
import io.reactivex.disposables.Disposable
import org.junit.Test
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicLong
import kotlin.properties.Delegates

class MyObservableTerminateOnValueOperator<T> constructor(val terminator: T) : ObservableOperator<T, T> {
    override fun apply(observer: Observer<in T>): Observer<in T> {
        return Op(observer, terminator)
    }

    class Op<T> constructor(val child: Observer<in T>, val terminator: T) : Observer<T> {

        var d: Disposable by Delegates.notNull()

        override fun onComplete() {
            println("onComplete")
            child.onComplete()
        }

        override fun onSubscribe(d: Disposable) {
            this.d = d
        }

        override fun onNext(t: T) {
            println("onNext : $t")
            if (t == terminator) {
                println("terminator found. disposing stream.")
                this.d.dispose()
            } else {
                child.onNext(t)
            }
        }

        override fun onError(t: Throwable) {
            println("onError : $t")
            child.onError(t)
        }
    }
}

class MyFlowableMoreThanOperator<T> constructor(val value: Long) : FlowableOperator<Boolean, T> {

    override fun apply(observer: Subscriber<in Boolean>): Subscriber<in T> {
        return Op<T>(observer, value)
    }

    class Op<T> constructor(val actual: Subscriber<in Boolean>, val value: Long) : Subscription, FlowableSubscriber<T> {
        var s: Subscription by Delegates.notNull()
        var counter = AtomicLong(0)

        override fun cancel() {
            println("cancel called.")
            s.cancel()
        }

        override fun request(n: Long) {
            println("request($n) called.")
            s.request(n)
        }

        override fun onNext(t: T) {
            val current = counter.incrementAndGet()
            if (current > value) {
                s.cancel()
                actual.onNext(true)
                actual.onComplete()
            }
        }

        override fun onComplete() {
            actual.onNext(false)
            actual.onComplete()
        }

        override fun onError(t: Throwable) {
            println("onError : $t")
            actual.onError(t)
        }

        override fun onSubscribe(s: Subscription) {
            println("onSubscribe")
            this.s = s
            actual.onSubscribe(s)
        }
    }
}

// Now /w kotlin extension.
fun <T> Observable<T>.terminateOn(terminator: T): Observable<T> {
    return this.lift(MyObservableTerminateOnValueOperator(terminator))
}

class CustomOperatorTest {

    @Test
    fun customOperatorTest() {

        // onNext : 1
        //  [19:49.965] [main] [1] subscribe = 1
        // onNext : 2
        //  [19:49.978] [main] [1] subscribe = 2
        // onNext : 3
        //  [19:49.978] [main] [1] subscribe = 3
        // onNext : 4
        // terminator found. disposing stream.
        // onNext : 1
        //  [19:49.979] [main] [2] subscribe = 1
        // onNext : 2
        //  [19:49.979] [main] [2] subscribe = 2
        // onNext : 3
        // terminator found. disposing stream.

        Observable.just(1, 2, 3, 4, 5)
                .lift(MyObservableTerminateOnValueOperator(4))
                .subscribe { pp("[1] subscribe = $it") }

        Observable.just(1, 2, 3, 4, 5)
                .terminateOn(3)
                .subscribe { pp("[2] subscribe = $it") }
    }

    @Test
    fun customFlowableOperatorTest() {

        // onSubscribe
        //  [20:46.412] [main] [1] Emitting 0
        //  [20:46.425] [main] [1] Emitting 1
        //  [20:46.425] [main] [1] Emitting 2
        //  [20:46.425] [main] [1] Emitting 3
        //  [20:46.426] [main] [1] result = true
        // onSubscribe
        //  [20:46.427] [main] [2] Emitting 0
        //  [20:46.427] [main] [2] Emitting 1
        //  [20:46.428] [main] [2] Emitting 2
        //  [20:46.428] [main] [2] Emitting 3
        //  [20:46.428] [main] [2] Emitting 4
        //  [20:46.428] [main] [2] Emitting onComplete
        //  [20:46.428] [main] result = false

        Flowable.range(0, 5)
                .doOnNext { pp("[1] Emitting $it") }
                .doOnComplete { pp("[1] Emitting onComplete") }
                .lift(MyFlowableMoreThanOperator(3))
                .subscribe { pp("[1] result = $it") }

        Flowable.range(0, 5)
                .doOnNext { pp("[2] Emitting $it") }
                .doOnComplete { pp("[2] Emitting onComplete") }
                .lift(MyFlowableMoreThanOperator(6))
                .subscribe { pp("result = $it") }
    }
}

