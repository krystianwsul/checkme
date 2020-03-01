package com.krystianwsul.common.utils

@Parcelize
data class TaskKey(val remoteProjectId: ProjectKey, val remoteTaskId: String) : Parcelable, Serializable {

    companion object {

        fun fromShortcut(shortcut: String): TaskKey {
            val (type, projectId, taskId) = shortcut.split(':')

            val projectKey: ProjectKey = when (ProjectKey.Type.valueOf(type)) {
                ProjectKey.Type.SHARED -> ProjectKey.Shared(projectId)
                ProjectKey.Type.PRIVATE -> ProjectKey.Private(projectId)
            }

            return TaskKey(projectKey, taskId)
        }
    }

    fun toShortcut() = "${remoteProjectId.type}$remoteProjectId:$remoteTaskId"
}
