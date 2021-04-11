package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey

class RootInstanceRecord<T : ProjectType>(
        create: Boolean,
        taskRecord: TaskRecord<T>,
        createObject: InstanceJson,
        scheduleKey: ScheduleKey,
        firebaseKey: String,
        private val parent: Parent,
) : InstanceRecord<T>(
        create,
        taskRecord,
        createObject,
        scheduleKey,
        firebaseKey,
) {

    constructor(
            taskRecord: TaskRecord<T>,
            createObject: InstanceJson,
            scheduleKey: ScheduleKey,
            parent: Parent,
    ) : this(
            true,
            taskRecord,
            createObject,
            scheduleKey,
            "${scheduleKeyToDateString(scheduleKey, true)}/${scheduleKeyToTimeString(scheduleKey, true)}",
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
            parent,
    )

    override fun deleteFromParent() = parent.removeRootInstanceRecord(instanceKey)

    interface Parent {

        fun removeRootInstanceRecord(instanceKey: InstanceKey)
    }
}