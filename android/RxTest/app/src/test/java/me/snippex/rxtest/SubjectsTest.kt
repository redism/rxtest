package me.snippex.rxtest

import io.reactivex.subjects.AsyncSubject
import org.junit.Test

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

        Thread.sleep(1000)
    }
}

