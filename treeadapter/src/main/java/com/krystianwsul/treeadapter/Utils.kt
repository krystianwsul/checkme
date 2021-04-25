package com.krystianwsul.treeadapter

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

fun <T : Any> Observable<T>.tryGetCurrentValue(): T? {
    var value: T? = null
    subscribe { t -> value = t }.dispose()
    return value
}

fun <T : Any> Observable<T>.getCurrentValue() = tryGetCurrentValue()!!

fun <T : Any> Single<T>.tryGetCurrentValue(): T? {
    var value: T? = null
    subscribe { t -> value = t }.dispose()
    return value
}

fun <T : Any> Single<T>.getCurrentValue() = tryGetCurrentValue()!!