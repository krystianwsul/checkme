package com.krystianwsul.common.utils

expect interface Serializable
expect annotation class Parcelize()
expect interface Parcelable

expect fun String.toBase64(): String

expect fun log(message: String)

expect fun <T> MutableList<T>.synchronized(): MutableList<T>

expect fun getThreadInfo(): ThreadInfo