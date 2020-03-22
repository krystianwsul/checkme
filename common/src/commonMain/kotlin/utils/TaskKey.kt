package com.krystianwsul.common.utils

@Parcelize
data class TaskKey(val projectId: ProjectKey<*>, val taskId: String) : Parcelable, Serializable {

    companion object {

        fun fromShortcut(shortcut: String): TaskKey {
            val (type, projectId, taskId) = shortcut.split(':')

            val projectKey: ProjectKey<*> = ProjectKey.Type
                    .valueOf(type)
                    .newKey(projectId)

            return TaskKey(projectKey, taskId)
        }
    }

    fun toShortcut() = "${projectId.type}:${projectId.key}:$taskId"
}
