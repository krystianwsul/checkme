package com.krystianwsul.common.utils

sealed class CustomTimeKey<T : ProjectType> : Parcelable, Serializable {

    abstract val remoteProjectId: ProjectKey<T>
    abstract val customTimeId: CustomTimeId<T>

    @Parcelize
    data class Private(
            override val remoteProjectId: ProjectKey<ProjectType.Private>,
            override val customTimeId: CustomTimeId.Private
    ) : CustomTimeKey<ProjectType.Private>()

    @Parcelize
    data class Shared(
            override val remoteProjectId: ProjectKey<ProjectType.Shared>,
            override val customTimeId: CustomTimeId<ProjectType.Shared>
    ) : CustomTimeKey<ProjectType.Shared>()
}
