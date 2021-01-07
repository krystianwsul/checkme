package com.krystianwsul.treeadapter

import io.reactivex.rxjava3.core.Observable

fun <T> Observable<T>.tryGetCurrentValue(): T? {
    var value: T? = null
    subscribe { t -> value = t }.dispose()
    return value
}

fun <T> Observable<T>.getCurrentValue() = tryGetCurrentValue()!!