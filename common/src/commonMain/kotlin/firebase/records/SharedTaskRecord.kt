package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.firebase.json.SharedTaskJson
import com.krystianwsul.common.utils.ProjectType

class SharedTaskRecord private constructor(
        create: Boolean,
        id: String,
        private val sharedProjectRecord: SharedProjectRecord,
        private val taskJson: SharedTaskJson,
) : TaskRecord<ProjectType.Shared>(create, id, sharedProjectRecord, taskJson) {

    override val createObject: SharedTaskJson // because of duplicate functionality when converting local task
        get() {
            if (update != null)
                taskJson.instances = instanceRecords.entries
                        .associateBy({ InstanceRecord.scheduleKeyToString(it.key) }, { it.value.createObject })
                        .toMutableMap()

            val scheduleWrappers = HashMap<String, ScheduleWrapper>()

            for (singleScheduleRecord in singleScheduleRecords.values)
                scheduleWrappers[singleScheduleRecord.id] = singleScheduleRecord.createObject

            for (weeklyScheduleRecord in weeklyScheduleRecords.values)
                scheduleWrappers[weeklyScheduleRecord.id] = weeklyScheduleRecord.createObject

            for (monthlyDayScheduleRecord in monthlyDayScheduleRecords.values)
                scheduleWrappers[monthlyDayScheduleRecord.id] = monthlyDayScheduleRecord.createObject

            for (monthlyWeekScheduleRecord in monthlyWeekScheduleRecords.values)
                scheduleWrappers[monthlyWeekScheduleRecord.id] = monthlyWeekScheduleRecord.createObject

            for (yearlyScheduleRecord in yearlyScheduleRecords.values)
                scheduleWrappers[yearlyScheduleRecord.id] = yearlyScheduleRecord.createObject

            taskJson.schedules = scheduleWrappers

            taskJson.noScheduleOrParent = noScheduleOrParentRecords.mapValues { it.value.createObject }

            return taskJson
        }

    override var assignedTo
        get() = taskJson.assignedTo.keys
        set(value) = setProperty(taskJson::assignedTo, value.associateWith { true })

    constructor(id: String, projectRecord: SharedProjectRecord, taskJson: SharedTaskJson) : this(
            false,
            id,
            projectRecord,
            taskJson
    )

    constructor(projectRecord: SharedProjectRecord, taskJson: SharedTaskJson) : this(
            true,
            projectRecord.getTaskRecordId(),
            projectRecord,
            taskJson
    )

    override fun deleteFromParent() = check(sharedProjectRecord.taskRecords.remove(id) == this)
}
