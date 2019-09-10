package com.krystianwsul.common.utils

expect interface Serializable
expect annotation class Parcelize()
expect interface Parcelable

expect fun String.toBase64(): String