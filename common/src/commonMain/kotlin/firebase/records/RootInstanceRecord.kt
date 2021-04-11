package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey

class RootInstanceRecord<T : ProjectType>(
        create: Boolean,
        taskRecord: TaskRecord<T>,
        createObject: InstanceJson,
        scheduleKey: ScheduleKey,
        firebaseKey: String,
        scheduleCustomTimeId: CustomTimeId.Project<T>?,
        private val parent: Parent,
) : InstanceRecord<T>(
        create,
        taskRecord,
        createObject,
        scheduleKey,
        firebaseKey,
        scheduleCustomTimeId,
) {

    companion object {

        private val dateRegex = Regex("^(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)$")
        private val hourMinuteRegex = Regex("^(\\d\\d)-(\\d\\d)$")

        private fun dateStringToDate(dateString: String): Date {
            val result = dateRegex.find(dateString)!!

            val year = result.getInt(1)
            val month = result.getInt(2)
            val day = result.getInt(3)

            return Date(year, month, day)
        }

        private fun <T : ProjectType> timeStringToTime(
                projectRecord: ProjectRecord<T>,
                timeString: String,
        ): Pair<TimePair, CustomTimeId.Project<T>?> {
            val result = hourMinuteRegex.find(timeString)

            return if (result != null) {
                val hour = result.getInt(1)
                val minute = result.getInt(2)

                Pair(TimePair(HourMinute(hour, minute)), null)
            } else {
                val customTimeKey = projectRecord.getCustomTimeKey(timeString)

                Pair(TimePair(customTimeKey), customTimeKey.customTimeId)
            }
        }

        // todo customtime use jsontime return scheduleKey and JsonTime
        fun <T : ProjectType> dateTimeStringsToSchedulePair(
                projectRecord: ProjectRecord<T>,
                dateString: String,
                timeString: String,
        ): Pair<ScheduleKey, CustomTimeId.Project<T>?> {
            val (timePair, customTimeId) = timeStringToTime(projectRecord, timeString)
            val scheduleKey = ScheduleKey(dateStringToDate(dateString), timePair)

            return Pair(scheduleKey, customTimeId)
        }
    }

    constructor(
            taskRecord: TaskRecord<T>,
            createObject: InstanceJson,
            scheduleKey: ScheduleKey,
            scheduleCustomTimeId: CustomTimeId.Project<T>?,
            parent: Parent,
    ) : this(
            true,
            taskRecord,
            createObject,
            scheduleKey,
            "${scheduleKeyToDateString(scheduleKey, true)}/${scheduleKeyToTimeString(scheduleKey, true)}",
            scheduleCustomTimeId,
            parent,
    )

    constructor(
            taskRecord: TaskRecord<T>,
            createObject: InstanceJson,
            dateString: String,
            timeString: String,
            parent: Parent,
            schedulePair: Pair<ScheduleKey, CustomTimeId.Project<T>?> = dateTimeStringsToSchedulePair(
                    taskRecord.projectRecord,
                    dateString,
                    timeString,
            ),
    ) : this(
            false,
            taskRecord,
            createObject,
            schedulePair.first,
            "$dateString/$timeString",
            schedulePair.second,
            parent
    )

    override fun deleteFromParent() = parent.removeRootInstanceRecord(instanceKey)

    interface Parent {

        fun removeRootInstanceRecord(instanceKey: InstanceKey)
    }
}