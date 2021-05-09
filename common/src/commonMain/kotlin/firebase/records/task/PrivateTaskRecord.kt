package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.json.noscheduleorparent.ProjectNoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.firebase.json.tasks.PrivateTaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.project.PrivateProjectRecord

class PrivateTaskRecord private constructor(
    create: Boolean,
    id: String,
    privateProjectRecord: PrivateProjectRecord,
    private val taskJson: PrivateTaskJson,
) : ProjectTaskRecord(create, id, privateProjectRecord, taskJson, AssignedToHelper.Private) {

    override val createObject: PrivateTaskJson // because of duplicate functionality when converting local task
        get() {
            if (update != null)
                taskJson.instances = instanceRecords.entries
                    .associateBy({ InstanceRecord.scheduleKeyToString(it.key) }, { it.value.createObject })
                    .toMutableMap()

            val scheduleWrappers = HashMap<String, PrivateScheduleWrapper>()

            for (singleScheduleRecord in singleScheduleRecords.values)
                scheduleWrappers[singleScheduleRecord.id] = singleScheduleRecord.createObject as PrivateScheduleWrapper

            for (weeklyScheduleRecord in weeklyScheduleRecords.values)
                scheduleWrappers[weeklyScheduleRecord.id] = weeklyScheduleRecord.createObject as PrivateScheduleWrapper

            for (monthlyDayScheduleRecord in monthlyDayScheduleRecords.values)
                scheduleWrappers[monthlyDayScheduleRecord.id] =
                    monthlyDayScheduleRecord.createObject as PrivateScheduleWrapper

            for (monthlyWeekScheduleRecord in monthlyWeekScheduleRecords.values)
                scheduleWrappers[monthlyWeekScheduleRecord.id] =
                    monthlyWeekScheduleRecord.createObject as PrivateScheduleWrapper

            for (yearlyScheduleRecord in yearlyScheduleRecords.values)
                scheduleWrappers[yearlyScheduleRecord.id] = yearlyScheduleRecord.createObject as PrivateScheduleWrapper

            taskJson.schedules = scheduleWrappers

            taskJson.noScheduleOrParent =
                noScheduleOrParentRecords.mapValues { it.value.createObject as ProjectNoScheduleOrParentJson } // todo task

            taskJson.taskHierarchies = taskHierarchyRecords.values.associate { it.id.value to it.createObject }

            return taskJson
        }

    override var startTimeOffset by Committer(taskJson::startTimeOffset)

    constructor(id: String, projectRecord: PrivateProjectRecord, taskJson: PrivateTaskJson) : this(
        false,
        id,
        projectRecord,
        taskJson,
    )

    constructor(projectRecord: PrivateProjectRecord, taskJson: PrivateTaskJson) : this(
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
    ) = PrivateScheduleWrapper(
        singleScheduleJson as? PrivateSingleScheduleJson,
        weeklyScheduleJson as? PrivateWeeklyScheduleJson,
        monthlyDayScheduleJson as? PrivateMonthlyDayScheduleJson,
        monthlyWeekScheduleJson as? PrivateMonthlyWeekScheduleJson,
        yearlyScheduleJson as? PrivateYearlyScheduleJson,
    )
}
