package com.krystianwsul.common.utils

sealed class CustomTimeKey<T : CustomTimeId, U : ProjectKey> : Parcelable, Serializable {

    abstract val remoteProjectId: U
    abstract val customTimeId: T

    @Parcelize
    data class Private(
            override val remoteProjectId: ProjectKey.Private,
            override val customTimeId: CustomTimeId.Private
    ) : CustomTimeKey<CustomTimeId.Private, ProjectKey.Private>()

    @Parcelize
    data class Shared(
            override val remoteProjectId: ProjectKey.Shared,
            override val customTimeId: CustomTimeId.Shared
    ) : CustomTimeKey<CustomTimeId.Shared, ProjectKey.Shared>()
}
