package com.krystianwsul.common.utils

sealed class CustomTimeKey : Parcelable, Serializable {

    abstract val customTimeId: CustomTimeId

    abstract fun toJson(): String

    sealed class Project<T : ProjectType> : CustomTimeKey() {

        abstract val projectId: ProjectKey<T>
        abstract override val customTimeId: CustomTimeId.Project<T>

        override fun toJson() = customTimeId.value

        @Parcelize
        data class Private(
                override val projectId: ProjectKey<ProjectType.Private>,
                override val customTimeId: CustomTimeId.Project.Private,
        ) : CustomTimeKey.Project<ProjectType.Private>()

        @Parcelize
        data class Shared(
                override val projectId: ProjectKey<ProjectType.Shared>,
                override val customTimeId: CustomTimeId.Project<ProjectType.Shared>,
        ) : CustomTimeKey.Project<ProjectType.Shared>()
    }

    @Parcelize
    data class User(val userKey: UserKey, override val customTimeId: CustomTimeId.User) : CustomTimeKey() {

        companion object {

            private val userRegex = Regex("^([^,]+),([^,]+)$")

            fun tryFromJson(json: String): User? {
                val matchResult = userRegex.find(json) ?: return null

                val userKey = UserKey(matchResult.groupValues[1])
                val customTimeId = CustomTimeId.User(matchResult.groupValues[2])

                return User(userKey, customTimeId)
            }
        }

        override fun toJson() = "${userKey.key},${customTimeId.value}"
    }
}
