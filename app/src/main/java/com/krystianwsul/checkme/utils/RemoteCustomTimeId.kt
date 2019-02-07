package com.krystianwsul.checkme.utils

import java.io.Serializable

sealed class RemoteCustomTimeId : Serializable {

    abstract val value: String

    data class Private(override val value: String) : RemoteCustomTimeId() {

        override fun toString() = value
    }

    data class Shared(override val value: String) : RemoteCustomTimeId() {

        override fun toString() = value
    }
}