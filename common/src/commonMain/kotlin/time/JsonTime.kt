package com.krystianwsul.common.time

import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType

sealed class JsonTime {

    companion object {

        fun <T : ProjectType> fromJson(projectIdProvider: ProjectIdProvider<T>, json: String): JsonTime {
            HourMinute.tryFromJson(json)?.let { return Normal(it) }

            CustomTimeKey.User.tryFromJson(json)?.let { return Custom.User(it) }

            return Custom.Project(projectIdProvider.getCustomTimeId(json))
        }

        fun <T : ProjectType> fromTime(time: Time): JsonTime {
            @Suppress("UNCHECKED_CAST")
            return when (time) {
                is Time.Custom.Project<*> -> Custom.Project(time.key.customTimeId as CustomTimeId.Project<T>)
                is Time.Custom.User -> Custom.User(time.key)
                is Time.Normal -> Normal(time.hourMinute)
            }
        }
    }

    abstract fun toJson(): String

    sealed class Custom : JsonTime() {

        data class Project<T : ProjectType>(val id: CustomTimeId.Project<T>) : JsonTime() {

            override fun toJson() = id.toString()
        }

        data class User(val key: CustomTimeKey.User) : JsonTime() {

            override fun toJson() = key.toJson()
        }
    }

    data class Normal(val hourMinute: HourMinute) : JsonTime() {

        override fun toJson() = hourMinute.toJson()
    }

    interface ProjectIdProvider<T : ProjectType> {

        fun getCustomTimeId(id: String): CustomTimeId.Project<T>
    }
}