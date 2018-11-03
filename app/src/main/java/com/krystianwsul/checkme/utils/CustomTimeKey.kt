package com.krystianwsul.checkme.utils

import java.io.Serializable

sealed class CustomTimeKey : Serializable {

    data class LocalCustomTimeKey(val localCustomTimeId: Int) : CustomTimeKey() {

        override fun toString() = "LocalCustomTimeKey $localCustomTimeId"
    }

    data class RemoteCustomTimeKey(val remoteProjectId: String, val remoteCustomTimeId: String) : CustomTimeKey() {

        override fun toString() = "LocalCustomTimeKey $remoteProjectId/$remoteCustomTimeId"
    }
}
