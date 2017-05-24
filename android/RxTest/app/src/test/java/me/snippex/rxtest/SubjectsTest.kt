package me.snippex.rxtest

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class SubjectsTest {

    @Test
    fun asyncSubject() {
        // http://reactivex.io/RxJava/javadoc/rx/subjects/AsyncSubject.html
        // AsyncSubject 는 무조건 최종값을 반환해주는 것이다. 서버 response 같은 것에서 사용하면 괜찮지 않을까 싶기도 하다.
        val subject = AsyncSubject.create<Int>()

        subject.subscribe { pp("subscribe 1 : $it") }
        subject.onNext(1)
        pp("subject.value 1 = ${subject.value}")
        subject.subscribe { pp("subscribe 2 : $it") }
        subject.onNext(2)
        pp("subject.value 2 = ${subject.value}")
        subject.onComplete()
        pp("subject.value 3 = ${subject.value}")

        Thread.sleep(1000)
    }

    @Test
    fun behaviorSubject() {
        val subject = BehaviorSubject.create<Int>()
        subject.subscribe { pp("subscribe 1 : $it") }
        pp("onNext(1)")
        subject.onNext(1)
        subject.subscribe { pp("subscribe 2 : $it") }
        pp("onNext(2)")
        subject.onNext(2)
        subject.onComplete()
    }

    @Test
    fun publishSubject() {
        val subject = PublishSubject.create<Int>()
        pp("onNext(0)")
        subject.onNext(0)
        subject.subscribe { pp("subscribe 1 : $it") }
        pp("onNext(1)")
        subject.onNext(1)
        subject.subscribe { pp("subscribe 2 : $it") }
        pp("onNext(2)")
        subject.onNext(2)
        subject.onComplete()
    }

    @Test
    fun replaySubject() {
        val subject = ReplaySubject.create<Int>()
        pp("onNext(0)")
        subject.onNext(0)
        subject.subscribe { pp("subscribe 1 : $it") }
        pp("onNext(1)")
        subject.onNext(1)
        subject.subscribe { pp("subscribe 2 : $it") }
        pp("onNext(2)")
        subject.onNext(2)
        subject.onComplete()
    }

    @Test
    fun unicastSubject() {
        val subject = UnicastSubject.create<Int>()
        pp("onNext(0)")
        subject.onNext(0)
        subject.subscribe { pp("subscribe 1 : $it") }
        pp("onNext(1)")
        subject.onNext(1)
//        subject.subscribe { pp("subscribe 2 : $it") } // This will occur error.
        pp("onNext(2)")
        subject.onNext(2)
        subject.onComplete()
    }

    @Test
    fun serializedSubject() {
        //
        // SerializedSubject 가 subject 의 thread-safety 를 보장해 준다고 하긴 하는데, 문제가 발생하는 상황을 정확하게 찾아내지는 못하겠다.
        // 아마도 대부분의 라이브러리 함수들은 안전하게 동작할 수 있게 처리되어 있어서 그런 것일 수도 있겠다. 만약 정확하게 문제 상황을 알면 도움이
        // 될 것 같긴 하다.
        //
        // http://reactivex.io/RxJava/javadoc/rx/subjects/SerializedSubject.html
        // https://stackoverflow.com/questions/31841809/is-serializedsubject-necessary-for-thread-safety-in-rxjava
        //
        val subject = PublishSubject.create<Long>()

        Observable.interval(1, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.newThread())
                .subscribe {
                    pp("onNext : $it")
                    subject.onNext(it)
                }

        Observable.interval(5, TimeUnit.MILLISECONDS)
                .take(1)
                .observeOn(Schedulers.io())
                .subscribe {
                    pp("onComplete")
                    subject.onComplete()
                }

        subject
                .scan { t1: Long, t2: Long ->
                    pp("$t1 , $t2")
                    t2
                }
                .blockingSubscribe()
    }
}

