package com.krystianwsul.common.utils

sealed class RemoteCustomTimeId : Parcelable, Serializable { // todo instance consider ProjectKey

    abstract val value: String

    @Parcelize
    data class Private(override val value: String) : RemoteCustomTimeId(), Comparable<Private> {

        override fun toString() = value

        override fun compareTo(other: Private) = value.compareTo(other.value)
    }

    @Parcelize
    data class Shared(override val value: String) : RemoteCustomTimeId() {

        override fun toString() = value
    }
}