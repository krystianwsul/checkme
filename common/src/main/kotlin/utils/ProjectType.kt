package com.krystianwsul.common.utils

sealed class ProjectType : Serializable, Parcelable {

    @Parcelize
    object Private : ProjectType()

    @Parcelize
    object Shared : ProjectType()
}