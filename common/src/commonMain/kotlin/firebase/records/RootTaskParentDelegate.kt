package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.RootTaskParentJson
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.invalidatableLazy

abstract class RootTaskParentDelegate(private val rootTaskParentJson: RootTaskParentJson) {

    companion object {

        private const val ROOT_TASK_IDS_KEY = "rootTaskIds"
    }

    private val rootTaskIds get() = rootTaskParentJson.rootTaskIds

    private val rootTaskKeysProperty = invalidatableLazy {
        rootTaskIds.keys
                .map(TaskKey::Root)
                .toSet()
    }
    val rootTaskKeys by rootTaskKeysProperty

    protected abstract fun addValue(subKey: String, value: Boolean?)

    fun addRootTask(rootTaskKey: TaskKey.Root) { // todo task fetch call into RootTaskKeySource
        val rootTaskId = rootTaskKey.taskId

        if (!rootTaskIds.containsKey(rootTaskId)) {
            rootTaskIds[rootTaskId] = true

            addValue("$ROOT_TASK_IDS_KEY/$rootTaskId", true)

            rootTaskKeysProperty.invalidate()
        }
    }

    fun removeRootTask(rootTaskKey: TaskKey.Root) { // todo task fetch call into RootTaskKeySource
        val rootTaskId = rootTaskKey.taskId

        if (rootTaskIds.containsKey(rootTaskId)) {
            rootTaskIds.remove(rootTaskId)

            addValue("$ROOT_TASK_IDS_KEY/$rootTaskId", null)

            rootTaskKeysProperty.invalidate()
        }
    }
}