package com.krystianwsul.common.utils

sealed class CustomTimeKey<T : ProjectType> : Parcelable, Serializable {

    abstract val projectId: ProjectKey<T>
    abstract val customTimeId: CustomTimeId.Project<T>

    @Parcelize
    data class Private(
            override val projectId: ProjectKey<ProjectType.Private>,
            override val customTimeId: CustomTimeId.Project.Private,
    ) : CustomTimeKey<ProjectType.Private>()

    @Parcelize
    data class Shared(
            override val projectId: ProjectKey<ProjectType.Shared>,
            override val customTimeId: CustomTimeId.Project<ProjectType.Shared>,
    ) : CustomTimeKey<ProjectType.Shared>()
}
