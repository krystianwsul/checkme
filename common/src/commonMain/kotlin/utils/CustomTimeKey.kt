package com.krystianwsul.common.utils

sealed class CustomTimeKey<T : RemoteCustomTimeId, U : ProjectKey> : Parcelable, Serializable {

    abstract val remoteProjectId: U
    abstract val remoteCustomTimeId: T

    @Parcelize
    data class Private(
            override val remoteProjectId: ProjectKey.Private,
            override val remoteCustomTimeId: RemoteCustomTimeId.Private
    ) : CustomTimeKey<RemoteCustomTimeId.Private, ProjectKey.Private>()

    @Parcelize
    data class Shared(
            override val remoteProjectId: ProjectKey.Shared,
            override val remoteCustomTimeId: RemoteCustomTimeId.Shared
    ) : CustomTimeKey<RemoteCustomTimeId.Shared, ProjectKey.Shared>()
}
