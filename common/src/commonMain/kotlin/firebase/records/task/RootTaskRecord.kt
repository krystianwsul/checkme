package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.TaskKey

class RootTaskRecord private constructor(
        create: Boolean,
        id: String,
        private val taskJson: RootTaskJson,
        private val databaseWrapper: DatabaseWrapper,
) : TaskRecord(
        create,
        id,
        taskJson,
        AssignedToHelper.root,
        JsonTime.ProjectCustomTimeIdAndKeyProvider.rootTask,
        id,
) {

    override val createObject: RootTaskJson // because of duplicate functionality when converting local task
        get() {
            if (update != null)
                taskJson.instances = instanceRecords.entries
                        .associateBy({ InstanceRecord.scheduleKeyToString(it.key) }, { it.value.createObject })
                        .toMutableMap()

            val scheduleWrappers = HashMap<String, RootScheduleWrapper>()

            for (singleScheduleRecord in singleScheduleRecords.values)
                scheduleWrappers[singleScheduleRecord.id] = singleScheduleRecord.createObject as RootScheduleWrapper

            for (weeklyScheduleRecord in weeklyScheduleRecords.values)
                scheduleWrappers[weeklyScheduleRecord.id] = weeklyScheduleRecord.createObject as RootScheduleWrapper

            for (monthlyDayScheduleRecord in monthlyDayScheduleRecords.values)
                scheduleWrappers[monthlyDayScheduleRecord.id] = monthlyDayScheduleRecord.createObject as RootScheduleWrapper

            for (monthlyWeekScheduleRecord in monthlyWeekScheduleRecords.values)
                scheduleWrappers[monthlyWeekScheduleRecord.id] = monthlyWeekScheduleRecord.createObject as RootScheduleWrapper

            for (yearlyScheduleRecord in yearlyScheduleRecords.values)
                scheduleWrappers[yearlyScheduleRecord.id] = yearlyScheduleRecord.createObject as RootScheduleWrapper

            taskJson.schedules = scheduleWrappers

            taskJson.noScheduleOrParent = noScheduleOrParentRecords.mapValues { it.value.createObject }

            taskJson.taskHierarchies = taskHierarchyRecords.mapValues { it.value.createObject }

            return taskJson
        }

    override val taskKey = TaskKey.Root(id)

    constructor(id: String, taskJson: RootTaskJson, databaseWrapper: DatabaseWrapper) :
            this(false, id, taskJson, databaseWrapper)

    constructor(taskJson: RootTaskJson, databaseWrapper: DatabaseWrapper) :
            this(true, databaseWrapper.newRootTaskRecordId(), taskJson, databaseWrapper)

    override fun getScheduleRecordId() = databaseWrapper.newRootTaskScheduleRecordId(id)
    override fun newNoScheduleOrParentRecordId() = databaseWrapper.newRootTaskNoScheduleOrParentRecordId(id)
    override fun newTaskHierarchyRecordId() = databaseWrapper.newRootTaskNestedTaskHierarchyRecordId(id)

    override fun deleteFromParent() {
        // todo task after fetch
    }

    override fun newScheduleWrapper(
            singleScheduleJson: SingleScheduleJson?,
            weeklyScheduleJson: WeeklyScheduleJson?,
            monthlyDayScheduleJson: MonthlyDayScheduleJson?,
            monthlyWeekScheduleJson: MonthlyWeekScheduleJson?,
            yearlyScheduleJson: YearlyScheduleJson?,
    ) = RootScheduleWrapper(
            singleScheduleJson as? RootSingleScheduleJson,
            weeklyScheduleJson as? RootWeeklyScheduleJson,
            monthlyDayScheduleJson as? RootMonthlyDayScheduleJson,
            monthlyWeekScheduleJson as? RootMonthlyWeekScheduleJson,
            yearlyScheduleJson as? RootYearlyScheduleJson,
    )
}