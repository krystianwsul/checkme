package com.krystianwsul.checkme.utils

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class TaskKey(val remoteProjectId: String, val remoteTaskId: String) : Parcelable, Serializable {

    companion object {

        fun fromShortcut(shortcut: String): TaskKey {
            val (projectId, taskId) = shortcut.split(':')

            return TaskKey(projectId, taskId)
        }
    }

    val type get() = Type.REMOTE

    fun toShortcut() = "$remoteProjectId:$remoteTaskId"

    enum class Type {
        REMOTE
    }
}
