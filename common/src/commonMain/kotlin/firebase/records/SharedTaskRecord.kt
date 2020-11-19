package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.SharedTaskJson
import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.utils.ProjectType

class SharedTaskRecord private constructor(
        create: Boolean,
        id: String,
        private val sharedProjectRecord: SharedProjectRecord,
        private val taskJson: SharedTaskJson,
) : TaskRecord<ProjectType.Shared>(create, id, sharedProjectRecord, taskJson, AssignedToHelper.Shared) {

    override val createObject: SharedTaskJson // because of duplicate functionality when converting local task
        get() {
            if (update != null)
                taskJson.instances = instanceRecords.entries
                        .associateBy({ InstanceRecord.scheduleKeyToString(it.key) }, { it.value.createObject })
                        .toMutableMap()

            val scheduleWrappers = HashMap<String, SharedScheduleWrapper>()

            for (singleScheduleRecord in singleScheduleRecords.values)
                scheduleWrappers[singleScheduleRecord.id] = singleScheduleRecord.createObject as SharedScheduleWrapper

            for (weeklyScheduleRecord in weeklyScheduleRecords.values)
                scheduleWrappers[weeklyScheduleRecord.id] = weeklyScheduleRecord.createObject as SharedScheduleWrapper

            for (monthlyDayScheduleRecord in monthlyDayScheduleRecords.values)
                scheduleWrappers[monthlyDayScheduleRecord.id] = monthlyDayScheduleRecord.createObject as SharedScheduleWrapper

            for (monthlyWeekScheduleRecord in monthlyWeekScheduleRecords.values)
                scheduleWrappers[monthlyWeekScheduleRecord.id] = monthlyWeekScheduleRecord.createObject as SharedScheduleWrapper

            for (yearlyScheduleRecord in yearlyScheduleRecords.values)
                scheduleWrappers[yearlyScheduleRecord.id] = yearlyScheduleRecord.createObject as SharedScheduleWrapper

            taskJson.schedules = scheduleWrappers

            taskJson.noScheduleOrParent = noScheduleOrParentRecords.mapValues { it.value.createObject }

            return taskJson
        }

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

    override fun newScheduleWrapper(
            singleScheduleJson: SingleScheduleJson<ProjectType.Shared>?,
            weeklyScheduleJson: WeeklyScheduleJson<ProjectType.Shared>?,
            monthlyDayScheduleJson: MonthlyDayScheduleJson<ProjectType.Shared>?,
            monthlyWeekScheduleJson: MonthlyWeekScheduleJson<ProjectType.Shared>?,
            yearlyScheduleJson: YearlyScheduleJson<ProjectType.Shared>?,
    ) = SharedScheduleWrapper(
            singleScheduleJson as? SharedSingleScheduleJson,
            weeklyScheduleJson as? SharedWeeklyScheduleJson,
            monthlyDayScheduleJson as? SharedMonthlyDayScheduleJson,
            monthlyWeekScheduleJson as? SharedMonthlyWeekScheduleJson,
            yearlyScheduleJson as? SharedYearlyScheduleJson
    )

    override fun deleteFromParent() = check(sharedProjectRecord.taskRecords.remove(id) == this)
}