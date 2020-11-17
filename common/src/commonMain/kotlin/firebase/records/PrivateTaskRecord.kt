package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.PrivateTaskJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType

class PrivateTaskRecord private constructor(
        create: Boolean,
        id: String,
        private val privateProjectRecord: PrivateProjectRecord,
        private val taskJson: PrivateTaskJson,
) : TaskRecord<ProjectType.Private>(create, id, privateProjectRecord, taskJson) {

    constructor(id: String, projectRecord: PrivateProjectRecord, taskJson: PrivateTaskJson) : this(
            false,
            id,
            projectRecord,
            taskJson
    )

    constructor(projectRecord: PrivateProjectRecord, taskJson: PrivateTaskJson) : this(
            true,
            projectRecord.getTaskRecordId(),
            projectRecord,
            taskJson
    )

    override val createObject: PrivateTaskJson // because of duplicate functionality when converting local task
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

    override fun deleteFromParent() = check(privateProjectRecord.taskRecords.remove(id) == this)
}
