package com.krystianwsul.common.time

import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectType

sealed class JsonTime<out T : ProjectType> {

    abstract fun toJson(): String

    data class Custom<T : ProjectType>(val id: CustomTimeId.Project<T>) : JsonTime<T>() {

        override fun toJson() = id.toString()
    }

    data class Normal<T : ProjectType>(val hourMinute: HourMinute) : JsonTime<T>() {

        override fun toJson() = hourMinute.toJson()
    }
}