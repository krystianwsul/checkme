package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.models.ProjectIdOwner
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey

abstract class ScheduleRecord(
    val taskRecord: TaskRecord,
    final override val createObject: ScheduleWrapper,
    private val scheduleJson: ScheduleJson,
    scheduleTypeSubkey: String,
    _id: String?,
    val projectRootDelegate: ProjectRootDelegate,
) : RemoteRecord(_id == null), ProjectIdOwner {

    companion object {

        const val SCHEDULES = "schedules"
    }

    val id = _id ?: taskRecord.getScheduleRecordId()

    final override val key get() = taskRecord.key + "/" + SCHEDULES + "/" + id

    val keyPlusSubkey = "$key/$scheduleTypeSubkey"

    val startTime get() = scheduleJson.startTime

    open var startTimeOffset
        get() = projectRootDelegate.startTimeOffset
        set(value) = projectRootDelegate.setStartTimeOffset(this, value)

    open var endTime by Committer(scheduleJson::endTime, keyPlusSubkey)
    open var endTimeOffset by Committer(scheduleJson::endTimeOffset, keyPlusSubkey)

    val taskId = taskRecord.id

    open val timePair get() = projectRootDelegate.timePair

    val customTimeKey: CustomTimeKey? get() = timePair.customTimeKey

    val assignedTo get() = taskRecord.assignedToHelper.getAssignedTo(scheduleJson)

    abstract val scheduleWrapperBridge: ScheduleWrapperBridge

    abstract val projectHelper: ProjectHelper

    val projectId get() = projectHelper.getProjectId(scheduleJson)

    override fun updateProject(projectKey: ProjectKey<*>) =
        projectHelper.setProjectId(scheduleJson, projectKey.key) { subKey, value ->
            addValue("$keyPlusSubkey/$subKey", value)
        }
}
