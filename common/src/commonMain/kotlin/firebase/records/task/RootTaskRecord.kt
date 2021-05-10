package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.noscheduleorparent.RootNoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.RootTaskParentDelegate
import com.krystianwsul.common.firebase.records.noscheduleorparent.RootNoScheduleOrParentRecord
import com.krystianwsul.common.firebase.records.schedule.*
import com.krystianwsul.common.firebase.records.taskhierarchy.NestedTaskHierarchyRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.TaskKey

class RootTaskRecord private constructor(
    create: Boolean,
    id: String,
    private val taskJson: RootTaskJson,
    private val databaseWrapper: DatabaseWrapper,
    parent: Parent,
) : TaskRecord(
    create,
    id,
    taskJson,
    AssignedToHelper.Shared,
    JsonTime.ProjectCustomTimeIdAndKeyProvider.rootTask,
    id,
    parent,
    ProjectHelper.Root,
    { taskRecord, scheduleJson ->
        ProjectRootDelegate.Root(taskRecord as RootTaskRecord, scheduleJson as RootScheduleJson)
    },
) {

    override val noScheduleOrParentRecords = taskJson.noScheduleOrParent
        .mapValues { RootNoScheduleOrParentRecord(this, it.value, it.key) }
        .toMutableMap()

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
                scheduleWrappers[monthlyDayScheduleRecord.id] =
                    monthlyDayScheduleRecord.createObject as RootScheduleWrapper

            for (monthlyWeekScheduleRecord in monthlyWeekScheduleRecords.values)
                scheduleWrappers[monthlyWeekScheduleRecord.id] =
                    monthlyWeekScheduleRecord.createObject as RootScheduleWrapper

            for (yearlyScheduleRecord in yearlyScheduleRecords.values)
                scheduleWrappers[yearlyScheduleRecord.id] = yearlyScheduleRecord.createObject as RootScheduleWrapper

            taskJson.schedules = scheduleWrappers

            taskJson.noScheduleOrParent = noScheduleOrParentRecords.mapValues { it.value.createObject }

            taskJson.taskHierarchies = taskHierarchyRecords.values.associate { it.id.value to it.createObject }

            return taskJson
        }

    override var name by Committer(taskJson::name)
    override var note by Committer(taskJson::note)
    override var image by Committer(taskJson::image)

    override val startTimeOffset get() = taskJson.startTimeOffset

    override val taskKey = TaskKey.Root(id)

    override val endData get() = taskJson.endData

    override fun setEndData(endData: RootTaskJson.EndData?) {
        if (endData == taskJson.endData) return

        setProperty(taskJson::endData, endData)
    }

    constructor(id: String, taskJson: RootTaskJson, databaseWrapper: DatabaseWrapper, parent: Parent) :
            this(false, id, taskJson, databaseWrapper, parent)

    constructor(taskJson: RootTaskJson, databaseWrapper: DatabaseWrapper, parent: Parent) :
            this(true, databaseWrapper.newRootTaskRecordId(), taskJson, databaseWrapper, parent)

    val rootTaskParentDelegate = object : RootTaskParentDelegate(taskJson) {

        override fun addValue(subKey: String, value: Boolean?) {
            this@RootTaskRecord.addValue("$key/$subKey", value)
        }
    }

    fun newNoScheduleOrParentRecord(noScheduleOrParentJson: RootNoScheduleOrParentJson): RootNoScheduleOrParentRecord {
        val noScheduleOrParentRecord = RootNoScheduleOrParentRecord(
            this,
            noScheduleOrParentJson,
            null,
        )

        check(!noScheduleOrParentRecords.containsKey(noScheduleOrParentRecord.id))

        noScheduleOrParentRecords[noScheduleOrParentRecord.id] = noScheduleOrParentRecord
        return noScheduleOrParentRecord
    }

    override fun getScheduleRecordId() = databaseWrapper.newRootTaskScheduleRecordId(id)
    override fun newNoScheduleOrParentRecordId() = databaseWrapper.newRootTaskNoScheduleOrParentRecordId(id)
    override fun newTaskHierarchyRecordId() = databaseWrapper.newRootTaskNestedTaskHierarchyRecordId(id)

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

    fun newSingleScheduleRecord(singleScheduleJson: RootSingleScheduleJson): SingleScheduleRecord {
        val singleScheduleRecord = SingleScheduleRecord(
            this,
            newScheduleWrapper(singleScheduleJson = singleScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, singleScheduleJson),
            getScheduleRecordId(),
            true,
        )

        check(!singleScheduleRecords.containsKey(singleScheduleRecord.id))

        singleScheduleRecords[singleScheduleRecord.id] = singleScheduleRecord
        return singleScheduleRecord
    }

    fun newWeeklyScheduleRecord(weeklyScheduleJson: RootWeeklyScheduleJson): WeeklyScheduleRecord {
        val weeklyScheduleRecord = WeeklyScheduleRecord(
            this,
            newScheduleWrapper(weeklyScheduleJson = weeklyScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, weeklyScheduleJson),
            getScheduleRecordId(),
            true,
        )

        check(!weeklyScheduleRecords.containsKey(weeklyScheduleRecord.id))

        weeklyScheduleRecords[weeklyScheduleRecord.id] = weeklyScheduleRecord
        return weeklyScheduleRecord
    }

    fun newMonthlyDayScheduleRecord(monthlyDayScheduleJson: RootMonthlyDayScheduleJson): MonthlyDayScheduleRecord {
        val monthlyDayScheduleRecord = MonthlyDayScheduleRecord(
            this,
            newScheduleWrapper(monthlyDayScheduleJson = monthlyDayScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, monthlyDayScheduleJson),
            getScheduleRecordId(),
            true,
        )

        check(!monthlyDayScheduleRecords.containsKey(monthlyDayScheduleRecord.id))

        monthlyDayScheduleRecords[monthlyDayScheduleRecord.id] = monthlyDayScheduleRecord
        return monthlyDayScheduleRecord
    }

    fun newMonthlyWeekScheduleRecord(monthlyWeekScheduleJson: RootMonthlyWeekScheduleJson): MonthlyWeekScheduleRecord {
        val monthlyWeekScheduleRecord = MonthlyWeekScheduleRecord(
            this,
            newScheduleWrapper(monthlyWeekScheduleJson = monthlyWeekScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, monthlyWeekScheduleJson),
            getScheduleRecordId(),
            true,
        )

        check(!monthlyWeekScheduleRecords.containsKey(monthlyWeekScheduleRecord.id))

        monthlyWeekScheduleRecords[monthlyWeekScheduleRecord.id] = monthlyWeekScheduleRecord
        return monthlyWeekScheduleRecord
    }

    fun newYearlyScheduleRecord(yearlyScheduleJson: RootYearlyScheduleJson): YearlyScheduleRecord {
        val yearlyScheduleRecord = YearlyScheduleRecord(
            this,
            newScheduleWrapper(yearlyScheduleJson = yearlyScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, yearlyScheduleJson),
            getScheduleRecordId(),
            true,
        )

        check(!yearlyScheduleRecords.containsKey(yearlyScheduleRecord.id))

        yearlyScheduleRecords[yearlyScheduleRecord.id] = yearlyScheduleRecord
        return yearlyScheduleRecord
    }

    fun newTaskHierarchyRecord(taskHierarchyJson: NestedTaskHierarchyJson): NestedTaskHierarchyRecord {
        val taskHierarchyRecord = NestedTaskHierarchyRecord(this, taskHierarchyJson)
        check(!taskHierarchyRecords.containsKey(taskHierarchyRecord.id))

        taskHierarchyRecords[taskHierarchyRecord.id] = taskHierarchyRecord
        return taskHierarchyRecord
    }

    fun getDependentTaskKeys(): Set<TaskKey.Root> =
        rootTaskParentDelegate.rootTaskKeys + taskHierarchyRecords.map { TaskKey.Root(it.value.parentTaskId) }
}
