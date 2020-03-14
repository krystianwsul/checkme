package com.krystianwsul.common.utils

@Parcelize
data class TaskKey(val remoteProjectId: ProjectKey, val remoteTaskId: String) : Parcelable, Serializable {

    companion object {

        fun fromShortcut(shortcut: String): TaskKey {
            val (type, projectId, taskId) = shortcut.split(':')

            val projectKey: ProjectKey = ProjectKey.Type
                    .valueOf(type)
                    .newKey(projectId)

            return TaskKey(projectKey, taskId)
        }
    }

    fun toShortcut() = "${remoteProjectId.type}:${remoteProjectId.key}:$remoteTaskId"
}
