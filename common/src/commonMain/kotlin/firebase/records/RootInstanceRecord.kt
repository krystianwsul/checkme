package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.JsonTime
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

        private fun dateStringToDate(dateString: String): Date {
            val result = dateRegex.find(dateString)!!

            val year = result.getInt(1)
            val month = result.getInt(2)
            val day = result.getInt(3)

            return Date(year, month, day)
        }

        // todo customtime cleanup (presumably will be using JsonTime eventually)
        fun <T : ProjectType> dateTimeStringsToScheduleKey(
                projectRecord: ProjectRecord<T>,
                dateString: String,
                timeString: String,
        ): ScheduleKey {
            val jsonTime = JsonTime.fromJson(projectRecord, timeString)

            return ScheduleKey(dateStringToDate(dateString), jsonTime.toTimePair(projectRecord))
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
            scheduleKey: ScheduleKey = dateTimeStringsToScheduleKey(
                    taskRecord.projectRecord,
                    dateString,
                    timeString,
            ),
    ) : this(
            false,
            taskRecord,
            createObject,
            scheduleKey,
            "$dateString/$timeString",
            scheduleKey.scheduleTimePair.customTimeKey?.customTimeId as? CustomTimeId.Project<T>, // todo customtime use jsontime ready
            parent,
    )

    override fun deleteFromParent() = parent.removeRootInstanceRecord(instanceKey)

    interface Parent {

        fun removeRootInstanceRecord(instanceKey: InstanceKey)
    }
}