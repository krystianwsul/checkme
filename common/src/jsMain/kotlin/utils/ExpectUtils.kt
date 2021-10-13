package com.krystianwsul.common.utils

actual interface Serializable
actual annotation class Parcelize
actual interface Parcelable

external class Buffer(x: String) {

    fun toString(x: String): String
}

actual fun String.toBase64() = Buffer(this).toString("base64")
        .replace('+', '-')
        .replace('/', '_')
        .replace(Regex("/=+\\$/"), "")

actual fun log(message: String) {
    console.log(message)
}

actual fun <T> MutableList<T>.synchronized() = this

actual fun getThreadInfo() = ThreadInfo(0L, "JS thread")