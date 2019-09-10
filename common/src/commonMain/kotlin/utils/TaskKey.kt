package com.krystianwsul.common.utils

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
