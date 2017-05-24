package me.snippex.kotlin

import io.reactivex.Observable
import me.snippex.rxtest.print
import org.junit.Test

data class Money(val amount: Double, val currency: String)

operator fun Money.plus(money: Money): Money {
    assert(this.currency == money.currency)
    return Money(this.amount + money.amount, this.currency)
}

operator fun Money.plus(amount: Double): Money {
    return Money(this.amount + amount, this.currency)
}

infix fun Int.percentOf(money: Money) = Money(money.amount * this / 100, money.currency)

fun Int.range() = Observable.range(0, this)

class Profiler constructor(val prefix: String) {

    var c = 0

    fun run(body: Profiler.() -> Unit) {
        body()
    }

    fun count() {
        c++
    }

    fun log(msg: String) {
        println("$prefix $msg")
    }
}

class OperatorOverrideTest {

    @Test
    fun operatorOverride() {
        val m1 = Money(100.0, "$")
        val m2 = Money(50.0, "$")
        println(m1 + m2)
        println(m1 + 500.0)
    }

    @Test
    fun primitiveExtension() {
        val m1 = Money(100.0, "$")

        println(7.percentOf(m1))
        println(7 percentOf m1)
    }

    @Test
    fun sampleObservableWithPrimitiveExtension() {
        10.range().print()
    }

    @Test
    fun toOperator() {
        val pair: Pair<Int, String> = 10 to "Ten"
        val (num, str) = pair

        println("$num = $str")
    }

    @Test
    fun forInUntil() {
        for (index in 0 until 5) {
            println("$index")
        }
    }

    @Test
    fun lambdaWithReceiver() {
        val p = Profiler("sample")

        // we don't need "it"
        // 빌더 패턴을 만들어내는데 훨씬 더 쉽지 않을까 싶다.
        p.run {
            log("Hello")
            count()
            log("World")
            count()
        }

        println(">> ${p.c}")
    }
}

