package com.krystianwsul.common.utils

sealed class CustomTimeId<T : ProjectType> : Parcelable, Serializable {

    abstract val value: String

    @Parcelize
    data class Private(override val value: String) : CustomTimeId<ProjectType.Private>(), Comparable<Private> {

        override fun toString() = value

        override fun compareTo(other: Private) = value.compareTo(other.value)
    }

    @Parcelize
    data class Shared(override val value: String) : CustomTimeId<ProjectType.Shared>() {

        override fun toString() = value
    }
}