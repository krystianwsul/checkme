package com.krystianwsul.common.time

import com.krystianwsul.common.utils.RemoteCustomTimeId

sealed class JsonTime<out T : RemoteCustomTimeId> {

    abstract fun toJson(): String

    data class Custom<T : RemoteCustomTimeId>(val id: T) : JsonTime<T>() {

        override fun toJson() = id.toString()
    }

    data class Normal<T : RemoteCustomTimeId>(val hourMinute: HourMinute) : JsonTime<T>() {

        override fun toJson() = hourMinute.toJson()
    }
}