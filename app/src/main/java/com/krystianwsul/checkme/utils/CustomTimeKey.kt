package com.krystianwsul.checkme.utils

import java.io.Serializable

sealed class CustomTimeKey : Serializable {

    data class LocalCustomTimeKey(val localCustomTimeId: Int) : CustomTimeKey()

    data class RemoteCustomTimeKey(val remoteProjectId: String, val remoteCustomTimeId: String) : CustomTimeKey()
}
