package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.schedule.ProjectHelper

abstract class ProjectTaskRecord protected constructor(
    create: Boolean,
    id: String,
    val projectRecord: ProjectRecord<*>,
    taskJson: TaskJson,
    assignedToHelper: AssignedToHelper,
) : TaskRecord(
    create,
    id,
    taskJson,
    assignedToHelper,
    projectRecord,
    projectRecord.childKey + "/" + TASKS + "/" + id,
    projectRecord,
    ProjectHelper.Project,
) {

    abstract override var startTimeOffset: Double?

    override val taskKey by lazy { projectRecord.getTaskKey(id) }

    override fun getScheduleRecordId() = projectRecord.getScheduleRecordId(id)
    override fun newNoScheduleOrParentRecordId() = projectRecord.newNoScheduleOrParentRecordId(id)
    override fun newTaskHierarchyRecordId() = projectRecord.newNestedTaskHierarchyRecordId(id)
}
