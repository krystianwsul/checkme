package com.krystianwsul.common.utils

sealed class ProjectKey : Parcelable, Serializable {

    abstract val key: String
    abstract val type: Type

    @Parcelize
    data class Shared(override val key: String) : ProjectKey(), Comparable<Shared> {

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

        SHARED {

            override fun newKey(key: String) = Shared(key)
        },
        PRIVATE {

            override fun newKey(key: String) = Private(key)
        };

        abstract fun newKey(key: String): ProjectKey
    }
}