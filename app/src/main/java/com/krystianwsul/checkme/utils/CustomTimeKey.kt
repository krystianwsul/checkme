package com.krystianwsul.checkme.utils

import java.io.Serializable

sealed class CustomTimeKey : Serializable {

    data class LocalCustomTimeKey(val localCustomTimeId: Int) : CustomTimeKey()

    data class RemoteCustomTimeKey<T : RemoteCustomTimeId>(val remoteProjectId: String, val remoteCustomTimeId: T) : CustomTimeKey() // todo add subclasses
}
