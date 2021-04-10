package com.krystianwsul.common.time

import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

sealed class JsonTime {

    companion object {

        private val hourMinuteRegex = Regex("^\\d\\d:\\d\\d$")

        private val userRegex = Regex("^[^,]+,[^,]+$")

        fun <T : ProjectType> fromJson(projectIdProvider: ProjectIdProvider<T>, json: String): JsonTime {
            return if (hourMinuteRegex.find(json) != null) {
                Normal(HourMinute.fromJson(json))
            } else {
                val matchResult = userRegex.find(json)
                if (matchResult != null) {
                    val userKey = UserKey(matchResult.groupValues[1])
                    val customTimeId = CustomTimeId.User(matchResult.groupValues[2])

                    Custom.User(CustomTimeKey.User(userKey, customTimeId))
                } else {
                    Custom.Project(projectIdProvider.getCustomTimeId(json))
                }
            }
        }
    }

    abstract fun toJson(): String

    sealed class Custom : JsonTime() {

        data class Project<T : ProjectType>(val id: CustomTimeId.Project<T>) : JsonTime() {

            override fun toJson() = id.toString()
        }

        data class User(val key: CustomTimeKey.User) : JsonTime() {

            override fun toJson() = "${key.userKey.key},${key.customTimeId.value}"
        }
    }

    data class Normal(val hourMinute: HourMinute) : JsonTime() {

        override fun toJson() = hourMinute.toJson()
    }

    interface ProjectIdProvider<T : ProjectType> {

        fun getCustomTimeId(id: String): CustomTimeId.Project<T>
    }
}