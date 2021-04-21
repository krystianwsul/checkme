package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.TaskKey

abstract class RootTaskRecord protected constructor(
        create: Boolean,
        id: String,
        taskJson: RootTaskJson,
        private val databaseWrapper: DatabaseWrapper,
) : TaskRecord(
        create,
        id,
        taskJson,
        AssignedToHelper.root,
        JsonTime.ProjectCustomTimeIdAndKeyProvider.rootTask,
        id,
) {

    override val taskKey = TaskKey.Root(id)

    override fun getScheduleRecordId() = databaseWrapper.newRootTaskScheduleRecordId(id)
    override fun newNoScheduleOrParentRecordId() = databaseWrapper.newRootTaskNoScheduleOrParentRecordId(id)
    override fun newTaskHierarchyRecordId() = databaseWrapper.newRootTaskNestedTaskHierarchyRecordId(id)
}
