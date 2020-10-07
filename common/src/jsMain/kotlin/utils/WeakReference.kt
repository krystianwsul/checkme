package com.krystianwsul.common.utils

actual class WeakReference<T : Any> actual constructor(referred: T) {

    private var value: T? = referred

    actual fun get() = value

    actual fun clear() {
        value = null
    }
}