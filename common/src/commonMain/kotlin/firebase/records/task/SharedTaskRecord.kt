package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.firebase.json.tasks.SharedTaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord

class SharedTaskRecord private constructor(
        create: Boolean,
        id: String,
        sharedProjectRecord: SharedProjectRecord,
        private val taskJson: SharedTaskJson,
) : ProjectTaskRecord(create, id, sharedProjectRecord, taskJson, AssignedToHelper.Shared) {

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

            taskJson.taskHierarchies = taskHierarchyRecords.mapValues { it.value.createObject }

            return taskJson
        }

    override var startTimeOffset by Committer(taskJson::startTimeOffset)

    constructor(id: String, projectRecord: SharedProjectRecord, taskJson: SharedTaskJson) : this(
            false,
            id,
            projectRecord,
            taskJson,
    )

    constructor(projectRecord: SharedProjectRecord, taskJson: SharedTaskJson) : this(
            true,
            projectRecord.getTaskRecordId(),
            projectRecord,
            taskJson,
    )

    override fun newScheduleWrapper(
            singleScheduleJson: SingleScheduleJson?,
            weeklyScheduleJson: WeeklyScheduleJson?,
            monthlyDayScheduleJson: MonthlyDayScheduleJson?,
            monthlyWeekScheduleJson: MonthlyWeekScheduleJson?,
            yearlyScheduleJson: YearlyScheduleJson?,
    ) = SharedScheduleWrapper(
            singleScheduleJson as? SharedSingleScheduleJson,
            weeklyScheduleJson as? SharedWeeklyScheduleJson,
            monthlyDayScheduleJson as? SharedMonthlyDayScheduleJson,
            monthlyWeekScheduleJson as? SharedMonthlyWeekScheduleJson,
            yearlyScheduleJson as? SharedYearlyScheduleJson,
    )
}
