package me.snippex.rxtest

import io.reactivex.Observable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

fun pp(msg: String) {
    val date = Date()
    val strDateFormat = "mm:ss.SSS"
    val dateFormat = SimpleDateFormat(strDateFormat)
    val formattedDate = dateFormat.format(date)
    println(" [$formattedDate] [${Thread.currentThread().name}] $msg")
}

fun <T> generateObservable(list: List<T?>, delayInMS: Long = 1000): Observable<T> {
    return Observable.defer {
        val observables = arrayListOf<Observable<T>>()

        list.forEachIndexed { index, value ->
            value?.let {
                observables.add(Observable.just(value).delay(index * delayInMS, TimeUnit.MILLISECONDS))
            }
        }
        Observable.merge(observables.asIterable())
    }
}

fun generate(streamString: String, delayInMS: Long = 1000): Observable<Int> {
    return generateObservable(streamString.split(",").map { it.takeIf { it.isNotEmpty() }?.toIntOrNull() }, delayInMS)
}

fun <T> Observable<T>.testResults(vararg values: T) {
    this.test().await().assertResult(*values)
}

// TODO: rename as dump
fun <T> Observable<T>.print() {
    this.blockingForEach { pp("$it") }
}

fun interval(ms: Long): Observable<Int> {
    return Observable.interval(ms, TimeUnit.MILLISECONDS).map { it.toInt() }
}

fun <T> Observable<T>.printNext(header: String = ""): Observable<T> {
    return this.doOnNext { pp("$header$it") }
}

fun <T> Observable<T>.printEach(header: String = ""): Observable<T> {
    return this.doOnEach { pp("$header$it") }
}
