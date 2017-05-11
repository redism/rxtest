package me.snippex.rxtest

import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Test

class RxSimpleTest {

    @Test
    @Throws(Exception::class)
    fun rxToBlockingShouldWork() {
        assertEquals(Observable.just(1).blockingFirst(), 1)
    }

}