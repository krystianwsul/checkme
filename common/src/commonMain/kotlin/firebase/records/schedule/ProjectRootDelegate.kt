package com.krystianwsul.common.firebase.records.schedule

import com.krystianwsul.common.firebase.json.schedule.ProjectScheduleJson
import com.krystianwsul.common.firebase.json.schedule.RootScheduleJson
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.TimePair

sealed class ProjectRootDelegate {

    companion object {

        private fun getTimePair(taskRecord: TaskRecord, time: String): TimePair {
            val jsonTime = JsonTime.fromJson(taskRecord.projectCustomTimeIdAndKeyProvider, time)

            return jsonTime.toTimePair(taskRecord.projectCustomTimeIdAndKeyProvider)
        }
    }

    abstract val startTimeOffset: Double?
    abstract fun setStartTimeOffset(scheduleRecord: ScheduleRecord, startTimeOffset: Double?)

    abstract val timePair: TimePair

    class Project(
        private val taskRecord: ProjectTaskRecord,
        private val scheduleJson: ProjectScheduleJson,
    ) : ProjectRootDelegate() {

        override val startTimeOffset get() = scheduleJson.startTimeOffset

        override fun setStartTimeOffset(scheduleRecord: ScheduleRecord, startTimeOffset: Double?) {
            if (startTimeOffset == this.startTimeOffset) return

            scheduleJson.startTimeOffset = startTimeOffset
            scheduleRecord.addValue("${scheduleRecord.keyPlusSubkey}/startTimeOffset", startTimeOffset)
        }

        override val timePair by lazy {
            scheduleJson.run {
                if (time != null) {
                    getTimePair(taskRecord, time!!)
                } else { // this part is only for old data
                    customTimeId?.let {
                        TimePair(taskRecord.projectRecord.getProjectCustomTimeKey(it))
                    } ?: TimePair(HourMinute(hour!!, minute!!))
                }
            }
        }
    }

    class Root(
        private val taskRecord: RootTaskRecord,
        private val scheduleJson: RootScheduleJson,
    ) : ProjectRootDelegate() {

        override val startTimeOffset get() = scheduleJson.startTimeOffset

        override fun setStartTimeOffset(scheduleRecord: ScheduleRecord, startTimeOffset: Double?) =
            throw UnsupportedOperationException()

        override val timePair by lazy { getTimePair(taskRecord, scheduleJson.time) }
    }
}