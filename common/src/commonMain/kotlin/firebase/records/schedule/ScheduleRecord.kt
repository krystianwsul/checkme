package com.krystianwsul.common.firebase.records.schedule


import com.krystianwsul.common.firebase.json.schedule.ScheduleJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeKey

abstract class ScheduleRecord(
        val taskRecord: TaskRecord,
        final override val createObject: ScheduleWrapper,
        private val scheduleJson: ScheduleJson,
        scheduleTypeSubkey: String,
        _id: String?,
) : RemoteRecord(_id == null) {

    companion object {

        const val SCHEDULES = "schedules"
    }

    val id = _id ?: taskRecord.getScheduleRecordId()

    final override val key get() = taskRecord.key + "/" + SCHEDULES + "/" + id

    val keyPlusSubkey = "$key/$scheduleTypeSubkey"

    val startTime get() = scheduleJson.startTime
    open var startTimeOffset by Committer(scheduleJson::startTimeOffset, keyPlusSubkey)

    open var endTime by Committer(scheduleJson::endTime, keyPlusSubkey)
    open var endTimeOffset by Committer(scheduleJson::endTimeOffset, keyPlusSubkey)

    val taskId = taskRecord.id

    open val timePair by lazy {
        scheduleJson.run {
            if (time != null) {
                val jsonTime = JsonTime.fromJson(taskRecord.projectCustomTimeIdAndKeyProvider, time!!)

                jsonTime.toTimePair(taskRecord.projectCustomTimeIdAndKeyProvider)
            } else { // this part is only for old data
                check(taskRecord is ProjectTaskRecord)

                customTimeId?.let {
                    TimePair(taskRecord.projectRecord.getProjectCustomTimeKey(it))
                } ?: TimePair(HourMinute(hour!!, minute!!))
            }
        }
    }

    val customTimeKey: CustomTimeKey? get() = timePair.customTimeKey

    val assignedTo get() = taskRecord.assignedToHelper.getAssignedTo(scheduleJson)

    abstract val scheduleWrapperBridge: ScheduleWrapperBridge
}
