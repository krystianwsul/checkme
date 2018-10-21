package com.krystianwsul.checkme.utils

import android.os.Parcelable
import android.text.TextUtils

import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class TaskKey(val localTaskId: Int?, val remoteProjectId: String?, val remoteTaskId: String?) : Parcelable, Serializable {

    val type
        get() = if (localTaskId != null) {
            check(TextUtils.isEmpty(remoteProjectId))
            check(TextUtils.isEmpty(remoteTaskId))

            Type.LOCAL
        } else {
            check(!TextUtils.isEmpty(remoteProjectId))
            check(!TextUtils.isEmpty(remoteTaskId))

            Type.REMOTE
        }

    constructor(localTaskId: Int) : this(localTaskId, null, null)

    constructor(remoteProjectId: String, remoteTaskId: String) : this(null, remoteProjectId, remoteTaskId)

    init {
        check(remoteProjectId.isNullOrEmpty() == remoteTaskId.isNullOrEmpty())
        check((localTaskId == null) != remoteProjectId.isNullOrEmpty())
    }

    override fun toString() = super.toString() + ": " + localTaskId + ", " + remoteProjectId + "/" + remoteTaskId

    enum class Type {
        LOCAL, REMOTE
    }
}
