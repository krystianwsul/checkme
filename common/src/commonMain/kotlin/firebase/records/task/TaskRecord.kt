package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.NoScheduleOrParentRecord
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.schedule.*
import com.krystianwsul.common.firebase.records.taskhierarchy.NestedTaskHierarchyRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ScheduleKey
import com.krystianwsul.common.utils.TaskKey

abstract class TaskRecord protected constructor(
        create: Boolean,
        val id: String,
        private val taskJson: TaskJson,
        val assignedToHelper: AssignedToHelper,
        val projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider,
        override val key: String,
        private val parent: Parent,
) : RemoteRecord(create) {

    companion object {

        const val TASKS = "tasks"
    }

    val instanceRecords = mutableMapOf<ScheduleKey, InstanceRecord>()

    val singleScheduleRecords: MutableMap<String, SingleScheduleRecord> = mutableMapOf()

    val weeklyScheduleRecords: MutableMap<String, WeeklyScheduleRecord> = mutableMapOf()

    val monthlyDayScheduleRecords: MutableMap<String, MonthlyDayScheduleRecord> = mutableMapOf()

    val monthlyWeekScheduleRecords: MutableMap<String, MonthlyWeekScheduleRecord> = mutableMapOf()

    val yearlyScheduleRecords: MutableMap<String, YearlyScheduleRecord> = mutableMapOf()

    val noScheduleOrParentRecords = taskJson.noScheduleOrParent
            .mapValues { NoScheduleOrParentRecord(this, it.value, it.key) }
            .toMutableMap()

    val taskHierarchyRecords = taskJson.taskHierarchies
            .mapValues { NestedTaskHierarchyRecord(it.key, this, it.value) }
            .toMutableMap()

    var name by Committer(taskJson::name)

    val startTime get() = taskJson.startTime
    var startTimeOffset by Committer(taskJson::startTimeOffset)

    abstract val taskKey: TaskKey

    var endData
        get() = taskJson.endData ?: taskJson.endTime?.let { TaskJson.EndData(it, null, false) }
        set(value) {
            if (value == taskJson.endData)
                return

            setProperty(taskJson::endData, value)
            setProperty(taskJson::endTime, value?.time)
        }

    var note by Committer(taskJson::note)

    var image by Committer(taskJson::image)

    var ordinal by Committer(taskJson::ordinal)

    final override val children
        get() = instanceRecords.values +
                singleScheduleRecords.values +
                weeklyScheduleRecords.values +
                monthlyDayScheduleRecords.values +
                monthlyWeekScheduleRecords.values +
                yearlyScheduleRecords.values +
                noScheduleOrParentRecords.values +
                taskHierarchyRecords.values

    init {
        for ((key, instanceJson) in taskJson.instances) {
            check(key.isNotEmpty())

            val scheduleKey = InstanceRecord.stringToScheduleKey(projectCustomTimeIdAndKeyProvider, key)

            val remoteInstanceRecord = InstanceRecord(
                    create,
                    this,
                    instanceJson,
                    scheduleKey,
                    key,
            )

            instanceRecords[scheduleKey] = remoteInstanceRecord
        }

        for ((id, scheduleWrapper) in taskJson.schedules) {
            check(id.isNotEmpty())

            val scheduleWrapperBridge = ScheduleWrapperBridge.fromScheduleWrapper(scheduleWrapper)

            when {
                scheduleWrapperBridge.singleScheduleJson != null -> {
                    check(scheduleWrapperBridge.weeklyScheduleJson == null)
                    check(scheduleWrapperBridge.monthlyDayScheduleJson == null)
                    check(scheduleWrapperBridge.monthlyWeekScheduleJson == null)
                    check(scheduleWrapperBridge.yearlyScheduleJson == null)

                    singleScheduleRecords[id] = SingleScheduleRecord(
                            this,
                            scheduleWrapper,
                            id,
                            scheduleWrapperBridge,
                    )
                }
                scheduleWrapperBridge.weeklyScheduleJson != null -> {
                    check(scheduleWrapperBridge.monthlyDayScheduleJson == null)
                    check(scheduleWrapperBridge.monthlyWeekScheduleJson == null)
                    check(scheduleWrapperBridge.yearlyScheduleJson == null)

                    weeklyScheduleRecords[id] = WeeklyScheduleRecord(
                            this,
                            scheduleWrapper,
                            id,
                            scheduleWrapperBridge,
                    )
                }
                scheduleWrapperBridge.monthlyDayScheduleJson != null -> {
                    check(scheduleWrapperBridge.monthlyWeekScheduleJson == null)
                    check(scheduleWrapperBridge.yearlyScheduleJson == null)

                    monthlyDayScheduleRecords[id] = MonthlyDayScheduleRecord(
                            this,
                            scheduleWrapper,
                            id,
                            scheduleWrapperBridge,
                    )
                }
                scheduleWrapperBridge.monthlyWeekScheduleJson != null -> {
                    check(scheduleWrapperBridge.yearlyScheduleJson == null)

                    monthlyWeekScheduleRecords[id] = MonthlyWeekScheduleRecord(
                            this,
                            scheduleWrapper,
                            id,
                            scheduleWrapperBridge,
                    )
                }
                else -> {
                    check(scheduleWrapperBridge.yearlyScheduleJson != null)

                    yearlyScheduleRecords[id] = YearlyScheduleRecord(
                            this,
                            scheduleWrapper,
                            id,
                            scheduleWrapperBridge,
                    )
                }
            }
        }
    }

    fun newInstanceRecord(instanceJson: InstanceJson, scheduleKey: ScheduleKey): InstanceRecord {
        val firebaseKey = InstanceRecord.scheduleKeyToString(scheduleKey)

        val projectInstanceRecord = InstanceRecord(
                true,
                this,
                instanceJson,
                scheduleKey,
                firebaseKey,
        )

        check(!instanceRecords.containsKey(projectInstanceRecord.scheduleKey))

        instanceRecords[projectInstanceRecord.scheduleKey] = projectInstanceRecord
        return projectInstanceRecord
    }

    protected abstract fun newScheduleWrapper(
            singleScheduleJson: SingleScheduleJson? = null,
            weeklyScheduleJson: WeeklyScheduleJson? = null,
            monthlyDayScheduleJson: MonthlyDayScheduleJson? = null,
            monthlyWeekScheduleJson: MonthlyWeekScheduleJson? = null,
            yearlyScheduleJson: YearlyScheduleJson? = null,
    ): ScheduleWrapper

    fun newSingleScheduleRecord(singleScheduleJson: SingleScheduleJson): SingleScheduleRecord {
        val singleScheduleRecord = SingleScheduleRecord(
                this,
                newScheduleWrapper(singleScheduleJson = singleScheduleJson),
        )

        check(!singleScheduleRecords.containsKey(singleScheduleRecord.id))

        singleScheduleRecords[singleScheduleRecord.id] = singleScheduleRecord
        return singleScheduleRecord
    }

    fun newWeeklyScheduleRecord(weeklyScheduleJson: WeeklyScheduleJson): WeeklyScheduleRecord {
        val weeklyScheduleRecord = WeeklyScheduleRecord(
                this,
                newScheduleWrapper(weeklyScheduleJson = weeklyScheduleJson),
        )

        check(!weeklyScheduleRecords.containsKey(weeklyScheduleRecord.id))

        weeklyScheduleRecords[weeklyScheduleRecord.id] = weeklyScheduleRecord
        return weeklyScheduleRecord
    }

    fun newMonthlyDayScheduleRecord(monthlyDayScheduleJson: MonthlyDayScheduleJson): MonthlyDayScheduleRecord {
        val monthlyDayScheduleRecord = MonthlyDayScheduleRecord(
                this,
                newScheduleWrapper(monthlyDayScheduleJson = monthlyDayScheduleJson),
        )

        check(!monthlyDayScheduleRecords.containsKey(monthlyDayScheduleRecord.id))

        monthlyDayScheduleRecords[monthlyDayScheduleRecord.id] = monthlyDayScheduleRecord
        return monthlyDayScheduleRecord
    }

    fun newMonthlyWeekScheduleRecord(monthlyWeekScheduleJson: MonthlyWeekScheduleJson): MonthlyWeekScheduleRecord {
        val monthlyWeekScheduleRecord = MonthlyWeekScheduleRecord(
                this,
                newScheduleWrapper(monthlyWeekScheduleJson = monthlyWeekScheduleJson),
        )

        check(!monthlyWeekScheduleRecords.containsKey(monthlyWeekScheduleRecord.id))

        monthlyWeekScheduleRecords[monthlyWeekScheduleRecord.id] = monthlyWeekScheduleRecord
        return monthlyWeekScheduleRecord
    }

    fun newYearlyScheduleRecord(yearlyScheduleJson: YearlyScheduleJson): YearlyScheduleRecord {
        val yearlyScheduleRecord = YearlyScheduleRecord(
                this,
                newScheduleWrapper(yearlyScheduleJson = yearlyScheduleJson),
        )

        check(!yearlyScheduleRecords.containsKey(yearlyScheduleRecord.id))

        yearlyScheduleRecords[yearlyScheduleRecord.id] = yearlyScheduleRecord
        return yearlyScheduleRecord
    }

    fun newNoScheduleOrParentRecord(noScheduleOrParentJson: NoScheduleOrParentJson): NoScheduleOrParentRecord {
        val noScheduleOrParentRecord = NoScheduleOrParentRecord(this, noScheduleOrParentJson, null)
        check(!noScheduleOrParentRecords.containsKey(noScheduleOrParentRecord.id))

        noScheduleOrParentRecords[noScheduleOrParentRecord.id] = noScheduleOrParentRecord
        return noScheduleOrParentRecord
    }

    fun newTaskHierarchyRecord(taskHierarchyJson: NestedTaskHierarchyJson): NestedTaskHierarchyRecord {
        val taskHierarchyRecord = NestedTaskHierarchyRecord(this, taskHierarchyJson)
        check(!taskHierarchyRecords.containsKey(taskHierarchyRecord.id))

        taskHierarchyRecords[taskHierarchyRecord.id] = taskHierarchyRecord
        return taskHierarchyRecord
    }

    abstract fun getScheduleRecordId(): String
    abstract fun newNoScheduleOrParentRecordId(): String
    abstract fun newTaskHierarchyRecordId(): String

    final override fun deleteFromParent() = parent.deleteTaskRecord(this)

    interface Parent {

        fun deleteTaskRecord(taskRecord: TaskRecord)
    }
}
