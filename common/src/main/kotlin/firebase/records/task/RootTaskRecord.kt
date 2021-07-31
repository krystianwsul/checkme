package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.noscheduleorparent.RootNoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.models.task.ProjectRootTaskIdTracker
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.RootTaskParentDelegate
import com.krystianwsul.common.firebase.records.noscheduleorparent.RootNoScheduleOrParentRecord
import com.krystianwsul.common.firebase.records.schedule.*
import com.krystianwsul.common.firebase.records.taskhierarchy.NestedTaskHierarchyRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.CustomTimeKey
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
        .mapValues { RootNoScheduleOrParentRecord(this, it.value, it.key, false) }
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
            this(true, databaseWrapper.newRootTaskRecordId(), taskJson, databaseWrapper, parent) {
        ProjectRootTaskIdTracker.checkTracking()
    }

    val rootTaskParentDelegate = object : RootTaskParentDelegate(taskJson) {

        override fun addValue(subKey: String, value: Boolean?) {
            this@RootTaskRecord.addValue("$key/$subKey", value)
        }
    }

    fun newNoScheduleOrParentRecord(noScheduleOrParentJson: RootNoScheduleOrParentJson): RootNoScheduleOrParentRecord {
        ProjectRootTaskIdTracker.checkTracking()

        val noScheduleOrParentRecord = RootNoScheduleOrParentRecord(
            this,
            noScheduleOrParentJson,
            databaseWrapper.newRootTaskNoScheduleOrParentRecordId(id),
            true,
        )

        check(!noScheduleOrParentRecords.containsKey(noScheduleOrParentRecord.id))

        noScheduleOrParentRecords[noScheduleOrParentRecord.id] = noScheduleOrParentRecord
        return noScheduleOrParentRecord
    }

    private fun newScheduleRecordId() = databaseWrapper.newRootTaskScheduleRecordId(id)
    fun newTaskHierarchyRecordId() = databaseWrapper.newRootTaskNestedTaskHierarchyRecordId(id)

    fun newSingleScheduleRecord(singleScheduleJson: RootSingleScheduleJson): SingleScheduleRecord {
        ProjectRootTaskIdTracker.checkTracking()

        val singleScheduleRecord = SingleScheduleRecord(
            this,
            RootScheduleWrapper(singleScheduleJson = singleScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, singleScheduleJson),
            newScheduleRecordId(),
            true,
        )

        check(!singleScheduleRecords.containsKey(singleScheduleRecord.id))

        singleScheduleRecords[singleScheduleRecord.id] = singleScheduleRecord
        return singleScheduleRecord
    }

    fun newWeeklyScheduleRecord(weeklyScheduleJson: RootWeeklyScheduleJson): WeeklyScheduleRecord {
        ProjectRootTaskIdTracker.checkTracking()

        val weeklyScheduleRecord = WeeklyScheduleRecord(
            this,
            RootScheduleWrapper(weeklyScheduleJson = weeklyScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, weeklyScheduleJson),
            newScheduleRecordId(),
            true,
        )

        check(!weeklyScheduleRecords.containsKey(weeklyScheduleRecord.id))

        weeklyScheduleRecords[weeklyScheduleRecord.id] = weeklyScheduleRecord
        return weeklyScheduleRecord
    }

    fun newMonthlyDayScheduleRecord(monthlyDayScheduleJson: RootMonthlyDayScheduleJson): MonthlyDayScheduleRecord {
        ProjectRootTaskIdTracker.checkTracking()

        val monthlyDayScheduleRecord = MonthlyDayScheduleRecord(
            this,
            RootScheduleWrapper(monthlyDayScheduleJson = monthlyDayScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, monthlyDayScheduleJson),
            newScheduleRecordId(),
            true,
        )

        check(!monthlyDayScheduleRecords.containsKey(monthlyDayScheduleRecord.id))

        monthlyDayScheduleRecords[monthlyDayScheduleRecord.id] = monthlyDayScheduleRecord
        return monthlyDayScheduleRecord
    }

    fun newMonthlyWeekScheduleRecord(monthlyWeekScheduleJson: RootMonthlyWeekScheduleJson): MonthlyWeekScheduleRecord {
        ProjectRootTaskIdTracker.checkTracking()

        val monthlyWeekScheduleRecord = MonthlyWeekScheduleRecord(
            this,
            RootScheduleWrapper(monthlyWeekScheduleJson = monthlyWeekScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, monthlyWeekScheduleJson),
            newScheduleRecordId(),
            true,
        )

        check(!monthlyWeekScheduleRecords.containsKey(monthlyWeekScheduleRecord.id))

        monthlyWeekScheduleRecords[monthlyWeekScheduleRecord.id] = monthlyWeekScheduleRecord
        return monthlyWeekScheduleRecord
    }

    fun newYearlyScheduleRecord(yearlyScheduleJson: RootYearlyScheduleJson): YearlyScheduleRecord {
        ProjectRootTaskIdTracker.checkTracking()

        val yearlyScheduleRecord = YearlyScheduleRecord(
            this,
            RootScheduleWrapper(yearlyScheduleJson = yearlyScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, yearlyScheduleJson),
            newScheduleRecordId(),
            true,
        )

        check(!yearlyScheduleRecords.containsKey(yearlyScheduleRecord.id))

        yearlyScheduleRecords[yearlyScheduleRecord.id] = yearlyScheduleRecord
        return yearlyScheduleRecord
    }

    fun newTaskHierarchyRecord(taskHierarchyJson: NestedTaskHierarchyJson): NestedTaskHierarchyRecord {
        ProjectRootTaskIdTracker.checkTracking()

        val taskHierarchyRecord = NestedTaskHierarchyRecord(this, taskHierarchyJson)
        check(!taskHierarchyRecords.containsKey(taskHierarchyRecord.id))

        taskHierarchyRecords[taskHierarchyRecord.id] = taskHierarchyRecord
        return taskHierarchyRecord
    }

    fun getAllDependencyTaskKeys(): Set<TaskKey.Root> = rootTaskParentDelegate.rootTaskKeys + getDirectDependencyTaskKeys()

    fun getDirectDependencyTaskKeys(): Set<TaskKey.Root> {
        val hierarchyKeys = taskHierarchyRecords.map { TaskKey.Root(it.value.parentTaskId) }

        val instanceKeys = instanceRecords.values.mapNotNull { it.parentInstanceKey?.taskKey as? TaskKey.Root }

        return (hierarchyKeys + instanceKeys).toSet()
    }

    override fun getUserCustomTimeKeys() = getCustomTimeKeys().map { it as CustomTimeKey.User }.toSet()
}
