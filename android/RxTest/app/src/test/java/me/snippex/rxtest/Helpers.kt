package me.snippex.rxtest

import io.reactivex.Observable
import java.util.concurrent.TimeUnit

fun pp(msg: String) {
    println(" [${Thread.currentThread().name}] $msg")
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

