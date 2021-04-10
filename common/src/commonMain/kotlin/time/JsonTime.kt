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

    abstract fun <T : ProjectType> toTime(
            projectCustomTimeProvider: ProjectCustomTimeProvider<T>,
            userCustomTimeProvider: UserCustomTimeProvider,
    ): Time

    sealed class Custom : JsonTime() {

        data class Project<U : ProjectType>(val id: CustomTimeId.Project<U>) : JsonTime() {

            override fun toJson() = id.toString()

            @Suppress("UNCHECKED_CAST")
            override fun <T : ProjectType> toTime(
                    projectCustomTimeProvider: ProjectCustomTimeProvider<T>,
                    userCustomTimeProvider: UserCustomTimeProvider,
            ) = projectCustomTimeProvider.getProjectCustomTime(id as CustomTimeId.Project<T>)
        }

        data class User(val key: CustomTimeKey.User) : JsonTime() {

            override fun toJson() = key.toJson()

            override fun <T : ProjectType> toTime(
                    projectCustomTimeProvider: ProjectCustomTimeProvider<T>,
                    userCustomTimeProvider: UserCustomTimeProvider,
            ) = userCustomTimeProvider.getUserCustomTime(key)
        }
    }

    data class Normal(val hourMinute: HourMinute) : JsonTime() {

        override fun toJson() = hourMinute.toJson()

        override fun <T : ProjectType> toTime(
                projectCustomTimeProvider: ProjectCustomTimeProvider<T>,
                userCustomTimeProvider: UserCustomTimeProvider,
        ) = Time.Normal(hourMinute)
    }

    interface ProjectIdProvider<T : ProjectType> {

        fun getCustomTimeId(id: String): CustomTimeId.Project<T>
    }

    interface ProjectCustomTimeProvider<T : ProjectType> {

        fun getProjectCustomTime(projectCustomTimeId: CustomTimeId.Project<T>): Time.Custom.Project<T>
    }

    interface UserCustomTimeProvider {

        fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User
    }
}