package me.snippex.kotlin

import org.junit.Test

class PaymentRobot {
    fun amount(value: Long) {
        println("amount = $value")
    }

    fun recipient(value: String) {
        println("recipient = $value")
    }

    fun send() {
        println("SEND!")
    }
}

fun payment(body: PaymentRobot.() -> Unit) = PaymentRobot().apply(body)

class DSLTest {

    @Test fun basicDSLLike() {
        payment {
            amount(100)
            recipient("Jane")
            send()
        }
    }
}

