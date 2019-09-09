package com.krystianwsul.common.time

sealed class JsonTime<out T> {

    abstract fun toJson(): String

    data class Custom<T>(val id: T) : JsonTime<T>() {

        override fun toJson() = id.toString()
    }

    data class Normal<T>(val hourMinute: HourMinute) : JsonTime<T>() {

        override fun toJson() = hourMinute.toJson()
    }
}