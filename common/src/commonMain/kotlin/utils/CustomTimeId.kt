package com.krystianwsul.common.utils

sealed class CustomTimeId<T : ProjectType> : Parcelable, Serializable {

    abstract val value: String

    sealed class Project<T : ProjectType> : CustomTimeId<T>() {

        @Parcelize
        data class Private(override val value: String) : Project<ProjectType.Private>() {

            override fun toString() = value
        }

        @Parcelize
        data class Shared(override val value: String) : Project<ProjectType.Shared>() {

            override fun toString() = value
        }
    }
}