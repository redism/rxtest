package me.snippex.rxtest

import io.reactivex.Maybe
import org.junit.Test

class MayBeTest {

    @Test
    fun mayBeBasic() {
        Maybe.just(1)
                .map<Any> { v -> v!! + 1 }
                .filter { v -> v == 1 }
                .defaultIfEmpty(2)
                .test()
                .assertResult(2)
    }
}

