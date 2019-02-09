package com.krystianwsul.checkme.utils.time

sealed class JsonTime<out T> {

    data class Custom<T>(val id: T) : JsonTime<T>()
    data class Normal<T>(val hourMinute: HourMinute) : JsonTime<T>()
}