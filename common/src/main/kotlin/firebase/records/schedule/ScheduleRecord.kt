package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.models.ProjectIdOwner
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.ScheduleKey

abstract class ScheduleRecord(
    val taskRecord: TaskRecord,
    final override val createObject: ScheduleWrapper,
    private val scheduleJson: ScheduleJson,
    scheduleTypeSubkey: String,
    val id: ScheduleId,
    create: Boolean,
    val projectRootDelegate: ProjectRootDelegate,
) : RemoteRecord(create), ProjectIdOwner {

    companion object {

        const val SCHEDULES = "schedules"
    }

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

    val assignedTo by lazy { taskRecord.assignedToHelper.getAssignedTo(scheduleJson) }

    abstract val scheduleWrapperBridge: ScheduleWrapperBridge

    abstract val projectHelper: ProjectHelper

    val projectId get() = projectHelper.getProjectId(scheduleJson)

    val scheduleKey by lazy { ScheduleKey(taskRecord.taskKey, id) }

    override fun updateProject(projectKey: ProjectKey<*>) =
        projectHelper.setProjectKey(scheduleJson, projectKey) { subKey, value ->
            addValue("$keyPlusSubkey/$subKey", value)
        }
}
