package com.krystianwsul.common.utils

sealed class TaskKey : Parcelable, Serializable {

    companion object {

        fun fromShortcut(shortcut: String): TaskKey {
            return if (shortcut.contains(':')) {
                val (type, projectId, taskId) = shortcut.split(':')

                val projectKey: ProjectKey<*> = ProjectKey.Type
                        .valueOf(type)
                        .newKey(projectId)

                Project(projectKey, taskId)
            } else {
                Root(shortcut)
            }
        }
    }

    abstract val taskId: String

    abstract fun toShortcut(): String

    @Parcelize
    data class Project(val projectKey: ProjectKey<*>, override val taskId: String) : TaskKey() {

        override fun toShortcut() = "${projectKey.type}:${projectKey.key}:$taskId"
    }

    @Parcelize
    data class Root(override val taskId: String) : TaskKey() {

        override fun toShortcut() = taskId
    }
}
