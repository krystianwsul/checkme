package com.krystianwsul.common.utils

sealed class ProjectKey : Parcelable {

    abstract val key: String
    abstract val type: Type

    @Parcelize
    data class Shared(override val key: String) : ProjectKey(), Comparable<Shared>, Serializable {

        override val type get() = Type.SHARED

        override fun compareTo(other: Shared) = key.compareTo(other.key)
    }

    @Parcelize
    data class Private(override val key: String) : ProjectKey(), Comparable<Private> {

        override val type get() = Type.PRIVATE

        override fun compareTo(other: Private) = key.compareTo(other.key)

        fun toUserKey() = UserKey(key)
    }

    enum class Type {

        SHARED, PRIVATE
    }
}