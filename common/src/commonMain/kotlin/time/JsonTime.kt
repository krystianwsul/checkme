package com.krystianwsul.common.time

import com.krystianwsul.common.utils.CustomTimeId

sealed class JsonTime<out T : CustomTimeId> {

    abstract fun toJson(): String

    data class Custom<T : CustomTimeId>(val id: T) : JsonTime<T>() {

        override fun toJson() = id.toString()
    }

    data class Normal<T : CustomTimeId>(val hourMinute: HourMinute) : JsonTime<T>() {

        override fun toJson() = hourMinute.toJson()
    }
}