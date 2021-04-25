package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.managers.MapRecordManager
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.utils.TaskKey

class RootTaskManager(private val databaseWrapper: DatabaseWrapper) :
        MapRecordManager<TaskKey.Root, RootTaskRecord>(), TaskRecord.Parent {

    companion object {

        private fun Snapshot<RootTaskJson>.toKey() = TaskKey.Root(key)
    }

    override val databasePrefix = DatabaseWrapper.TASKS_KEY

    override fun valueToRecord(value: RootTaskRecord) = value

    fun set(snapshot: Snapshot<RootTaskJson>) = set(
            snapshot.toKey(),
            { it.createObject != snapshot.value },
            {
                snapshot.takeIf { it.exists }?.let {
                    RootTaskRecord(
                            it.key,
                            it.value!!,
                            databaseWrapper,
                            this,
                    )
                }
            },
    )?.data

    override fun deleteTaskRecord(taskRecord: TaskRecord) = remove((taskRecord as RootTaskRecord).taskKey)
}