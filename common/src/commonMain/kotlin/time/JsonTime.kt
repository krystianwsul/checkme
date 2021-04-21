package com.krystianwsul.common.time

import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType

sealed class JsonTime {

    companion object {

        fun fromJson(projectCustomTimeIdProvider: ProjectCustomTimeIdProvider, json: String): JsonTime {
            HourMinute.tryFromJson(json)?.let { return Normal(it) }

            return Custom.fromJson(projectCustomTimeIdProvider, json)
        }

        fun fromTime(time: Time): JsonTime {
            return when (time) {
                is Time.Custom.Project<*> -> Custom.Project(time.key.customTimeId)
                is Time.Custom.User -> Custom.User(time.key)
                is Time.Normal -> Normal(time.hourMinute)
            }
        }

        fun fromTimePair(timePair: TimePair): JsonTime {
            timePair.hourMinute?.let { return Normal(it) }

            return Custom.fromCustomTimeKey(timePair.customTimeKey!!)
        }
    }

    abstract fun toJson(): String

    abstract fun getCustomTimeKey(projectCustomTimeKeyProvider: ProjectCustomTimeKeyProvider): CustomTimeKey?

    abstract fun toTimePair(projectCustomTimeKeyProvider: ProjectCustomTimeKeyProvider): TimePair

    abstract fun <T : ProjectType> toTime(
            projectCustomTimeProvider: ProjectCustomTimeProvider<T>,
            userCustomTimeProvider: UserCustomTimeProvider,
    ): Time

    fun toTime(customTimeProvider: CustomTimeProvider<*>) = toTime(customTimeProvider, customTimeProvider)

    sealed class Custom : JsonTime() {

        companion object {

            fun fromJson(projectCustomTimeIdProvider: ProjectCustomTimeIdProvider, json: String): JsonTime {
                CustomTimeKey.User.tryFromJson(json)?.let { return User(it) }

                return Project(projectCustomTimeIdProvider.getProjectCustomTimeId(json))
            }

            fun fromCustomTimeKey(customTimeKey: CustomTimeKey): Custom {
                return when (customTimeKey) {
                    is CustomTimeKey.Project<*> -> Project(customTimeKey.customTimeId)
                    is CustomTimeKey.User -> User(customTimeKey)
                }
            }
        }

        abstract override fun getCustomTimeKey(
                projectCustomTimeKeyProvider: ProjectCustomTimeKeyProvider,
        ): CustomTimeKey

        override fun toTimePair(
                projectCustomTimeKeyProvider: ProjectCustomTimeKeyProvider,
        ) = TimePair(getCustomTimeKey(projectCustomTimeKeyProvider), null)

        data class Project(val id: CustomTimeId.Project) : JsonTime.Custom() {

            override fun toJson() = id.toString()

            override fun <T : ProjectType> toTime(
                    projectCustomTimeProvider: ProjectCustomTimeProvider<T>,
                    userCustomTimeProvider: UserCustomTimeProvider,
            ) = projectCustomTimeProvider.getProjectCustomTime(id)

            override fun getCustomTimeKey(
                    projectCustomTimeKeyProvider: ProjectCustomTimeKeyProvider,
            ) = projectCustomTimeKeyProvider.getProjectCustomTimeKey(id)
        }

        data class User(val key: CustomTimeKey.User) : JsonTime.Custom() {

            override fun toJson() = key.toJson()

            override fun <T : ProjectType> toTime(
                    projectCustomTimeProvider: ProjectCustomTimeProvider<T>,
                    userCustomTimeProvider: UserCustomTimeProvider,
            ) = userCustomTimeProvider.getUserCustomTime(key)

            override fun getCustomTimeKey(projectCustomTimeKeyProvider: ProjectCustomTimeKeyProvider) = key
        }
    }

    data class Normal(val hourMinute: HourMinute) : JsonTime() {

        override fun toJson() = hourMinute.toJson()

        override fun <T : ProjectType> toTime(
                projectCustomTimeProvider: ProjectCustomTimeProvider<T>,
                userCustomTimeProvider: UserCustomTimeProvider,
        ) = Time.Normal(hourMinute)

        override fun getCustomTimeKey(projectCustomTimeKeyProvider: ProjectCustomTimeKeyProvider): CustomTimeKey? = null

        override fun toTimePair(
                projectCustomTimeKeyProvider: ProjectCustomTimeKeyProvider,
        ) = TimePair(null, hourMinute)
    }

    interface ProjectCustomTimeIdProvider {

        fun getProjectCustomTimeId(id: String): CustomTimeId.Project
    }

    interface ProjectCustomTimeProvider<T : ProjectType> {

        fun getProjectCustomTime(projectCustomTimeId: CustomTimeId.Project): Time.Custom.Project<T>
    }

    interface ProjectCustomTimeKeyProvider {

        fun getProjectCustomTimeKey(projectCustomTimeId: CustomTimeId.Project): CustomTimeKey.Project<*>
    }

    interface ProjectCustomTimeIdAndKeyProvider : ProjectCustomTimeIdProvider, ProjectCustomTimeKeyProvider {

        companion object {

            val rootTask = object : ProjectCustomTimeIdAndKeyProvider {

                override fun getProjectCustomTimeId(id: String): CustomTimeId.Project = throw RootTaskException()

                override fun getProjectCustomTimeKey(
                        projectCustomTimeId: CustomTimeId.Project,
                ): CustomTimeKey.Project<*> = throw RootTaskException()
            }
        }

        class RootTaskException : Exception("This should never be called for root tasks")
    }

    interface UserCustomTimeProvider {

        fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User
    }

    interface CustomTimeProvider<T : ProjectType> : ProjectCustomTimeProvider<T>, UserCustomTimeProvider {

        fun getCustomTime(customTimeKey: CustomTimeKey): Time.Custom {
            @Suppress("UNCHECKED_CAST")
            return when (customTimeKey) {
                is CustomTimeKey.Project<*> ->
                    getProjectCustomTime((customTimeKey as CustomTimeKey.Project<T>).customTimeId)
                is CustomTimeKey.User -> getUserCustomTime(customTimeKey)
            }
        }
    }
}