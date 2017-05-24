package me.snippex.kotlin

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import me.snippex.rxtest.pp
import org.junit.Test
import kotlin.properties.Delegates

class Sample {
    val value by lazy {
        pp("value getter called.")
        100
    }

    var name by Delegates.observable("jane") { prop, old, new ->
        println("name changed from $old to $new, (property name = ${prop.name})")
    }

    var address by Delegates.notNull<Int>()
}

class DelegationPropertyTest {

    @Test
    fun byLazy() {
        Sample().value

        val s = Sample()
        Observable.just(1)
                .observeOn(Schedulers.newThread())
                .map { s.value }
                .test().await().assertValue(100)
    }

    @Test
    fun DelegatesObservable() {
        val s = Sample()
        s.name = "hello"
    }

    @Test
    fun DelegatesNotNull() {
        val s = Sample()
        s.address = 100
    }
}

