package com.krystianwsul.common.utils

sealed class CustomTimeKey : Parcelable, Serializable {

    abstract val customTimeId: CustomTimeId

    sealed class Project<T : ProjectType> : CustomTimeKey() {

        abstract val projectId: ProjectKey<T>
        abstract override val customTimeId: CustomTimeId.Project<T>

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
    data class User(val userKey: UserKey, override val customTimeId: CustomTimeId.User) : CustomTimeKey()
}
