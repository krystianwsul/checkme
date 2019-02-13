package com.krystianwsul.checkme.utils

import java.io.Serializable

sealed class CustomTimeKey<T : RemoteCustomTimeId> : Serializable {

    abstract val remoteProjectId: String
    abstract val remoteCustomTimeId: T

    data class Private(override val remoteProjectId: String, override val remoteCustomTimeId: RemoteCustomTimeId.Private) : CustomTimeKey<RemoteCustomTimeId.Private>()
    data class Shared(override val remoteProjectId: String, override val remoteCustomTimeId: RemoteCustomTimeId.Shared) : CustomTimeKey<RemoteCustomTimeId.Shared>()
}
