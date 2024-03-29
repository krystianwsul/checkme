package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.json.schedule.ProjectScheduleJson
import com.krystianwsul.common.firebase.json.tasks.ProjectTaskJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.noscheduleorparent.ProjectNoScheduleOrParentRecord
import com.krystianwsul.common.firebase.records.project.OwnedProjectRecord
import com.krystianwsul.common.firebase.records.schedule.ProjectHelper
import com.krystianwsul.common.firebase.records.schedule.ProjectRootDelegate
import com.krystianwsul.common.utils.CustomTimeKey

abstract class ProjectTaskRecord protected constructor(
    create: Boolean,
    id: String,
    val projectRecord: OwnedProjectRecord<*>,
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

    override val name get() = projectTaskJson.name
    override val note get() = projectTaskJson.note
    override val image get() = projectTaskJson.image

    abstract override var startTimeOffset: Double?

    override val taskKey by lazy { projectRecord.getTaskKey(id) }

    override val endData
        get() = projectTaskJson.endData ?: projectTaskJson.endTime?.let {
            ProjectTaskJson.EndData(it, null, false)
        }

    override fun setEndData(endData: RootTaskJson.EndData?) {
        val compatEndData = endData?.toCompat()

        if (compatEndData == projectTaskJson.endData) return

        setProperty(projectTaskJson::endData, compatEndData)
        setProperty(projectTaskJson::endTime, compatEndData?.time)
    }

    override fun getUserCustomTimeKeys() = getCustomTimeKeys().filterIsInstance<CustomTimeKey.User>().toSet()
}
