package com.krystianwsul.checkme.utils

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

sealed class RemoteCustomTimeId : Serializable {

    abstract val value: String

    @Parcelize
    data class Private(override val value: String) : RemoteCustomTimeId(), Parcelable, Comparable<Private> {

        override fun toString() = value

        override fun compareTo(other: Private) = value.compareTo(other.value)
    }

    data class Shared(override val value: String) : RemoteCustomTimeId() {

        override fun toString() = value
    }
}