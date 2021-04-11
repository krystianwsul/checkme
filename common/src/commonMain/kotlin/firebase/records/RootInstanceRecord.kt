package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.InstanceJson
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