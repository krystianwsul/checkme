package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.utils.TaskKey

abstract class RootTasksManager(protected val databaseWrapper: DatabaseWrapper) :
        MapRecordManager<TaskKey.Root, RootTaskRecord>(), TaskRecord.Parent {

    override val databasePrefix = DatabaseWrapper.TASKS_KEY

    override fun valueToRecord(value: RootTaskRecord) = value

    override fun deleteTaskRecord(taskRecord: TaskRecord) = remove((taskRecord as RootTaskRecord).taskKey)

    fun newTaskRecord(taskJson: RootTaskJson) =
            RootTaskRecord(taskJson, databaseWrapper, this).also { add(it.taskKey, it) }
}