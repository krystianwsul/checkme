package com.krystianwsul.checkme.utils

sealed class RemoteCustomTimeId {

    abstract val value: String

    data class Private(override val value: String) : RemoteCustomTimeId()
    data class Shared(override val value: String) : RemoteCustomTimeId()
}