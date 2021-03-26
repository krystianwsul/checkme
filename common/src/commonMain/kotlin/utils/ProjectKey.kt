package com.krystianwsul.common.utils

sealed class ProjectKey<T : ProjectType> : Parcelable, Serializable {

    abstract val key: String
    abstract val type: Type

    @Parcelize
    data class Private(override val key: String) : ProjectKey<ProjectType.Private>(), Comparable<Private> {

        override val type get() = Type.PRIVATE

        override fun compareTo(other: Private) = key.compareTo(other.key)

        fun toUserKey() = UserKey(key)
    }

    @Parcelize
    data class Shared(override val key: String) : ProjectKey<ProjectType.Shared>(), Comparable<Shared> {

        override val type get() = Type.SHARED

        override fun compareTo(other: Shared) = key.compareTo(other.key)
    }

    enum class Type {

        PRIVATE {

            override fun newKey(key: String) = Private(key)
        },
        SHARED {

            override fun newKey(key: String) = Shared(key)
        };

        abstract fun newKey(key: String): ProjectKey<*>
    }
}