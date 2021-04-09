package com.krystianwsul.common.time

import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectType

sealed class JsonTime<out T : ProjectType> {

    companion object {

        private val hourMinuteRegex = Regex("^\\d\\d:\\d\\d$")

        private val userRegex = Regex("^[^,]+,[^,]+$")

        fun <T : ProjectType> fromJson(projectIdProvider: ProjectIdProvider<T>, json: String): JsonTime<T> {
            return if (hourMinuteRegex.find(json) != null)
                Normal(HourMinute.fromJson(json))
            else
                Custom(projectIdProvider.getCustomTimeId(json))
        }
    }

    abstract fun toJson(): String

    data class Custom<T : ProjectType>(val id: CustomTimeId.Project<T>) : JsonTime<T>() {

        override fun toJson() = id.toString()
    }

    data class Normal<T : ProjectType>(val hourMinute: HourMinute) : JsonTime<T>() {

        override fun toJson() = hourMinute.toJson()
    }

    interface ProjectIdProvider<T : ProjectType> {

        fun getCustomTimeId(id: String): CustomTimeId.Project<T>
    }
}