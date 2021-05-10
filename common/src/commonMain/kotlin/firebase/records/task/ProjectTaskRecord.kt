package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.json.schedule.ProjectScheduleJson
import com.krystianwsul.common.firebase.json.tasks.ProjectTaskJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.noscheduleorparent.ProjectNoScheduleOrParentRecord
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.schedule.ProjectHelper
import com.krystianwsul.common.firebase.records.schedule.ProjectRootDelegate

abstract class ProjectTaskRecord protected constructor(
    create: Boolean,
    id: String,
    val projectRecord: ProjectRecord<*>,
    private val projectTaskJson: ProjectTaskJson,
    assignedToHelper: AssignedToHelper,
) : TaskRecord(
    create,
    id,
    projectTaskJson,
    assignedToHelper,
    projectRecord,
    projectRecord.childKey + "/" + TASKS + "/" + id,
    projectRecord,
    ProjectHelper.Project,
    { taskRecord, scheduleJson ->
        ProjectRootDelegate.Project(taskRecord as ProjectTaskRecord, scheduleJson as ProjectScheduleJson)
    },
) {

    override val noScheduleOrParentRecords = projectTaskJson.noScheduleOrParent
        .mapValues { ProjectNoScheduleOrParentRecord(this, it.value, it.key) }
        .toMutableMap()

    abstract override var startTimeOffset: Double?

    override val taskKey by lazy { projectRecord.getTaskKey(id) }

    override var endData
        get() = projectTaskJson.endData ?: projectTaskJson.endTime?.let {
            TaskJson.EndData(it, null, false)
        }
        set(value) {
            if (value == projectTaskJson.endData) return

            setProperty(projectTaskJson::endData, value)
            setProperty(projectTaskJson::endTime, value?.time)
        }

    override fun getScheduleRecordId() = projectRecord.getScheduleRecordId(id)
    override fun newNoScheduleOrParentRecordId() = projectRecord.newNoScheduleOrParentRecordId(id)
    override fun newTaskHierarchyRecordId() = projectRecord.newNestedTaskHierarchyRecordId(id)
}
