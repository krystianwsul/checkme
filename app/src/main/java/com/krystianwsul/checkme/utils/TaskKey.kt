package com.krystianwsul.checkme.utils

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class TaskKey(val remoteProjectId: String, val remoteTaskId: String) : Parcelable, Serializable {

    val type get() = Type.REMOTE

    override fun toString() = super.toString() + ": " + remoteProjectId + "/" + remoteTaskId

    enum class Type {
        REMOTE
    }
}
