package com.krystianwsul.common.utils

expect class WeakReference<T : Any>(referred: T) {

    fun get(): T?

    fun clear()
}