package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.firebase.records.schedule.*
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey

abstract class TaskRecord<T : ProjectType> protected constructor(
        create: Boolean,
        val id: String,
        val projectRecord: ProjectRecord<T>,
        private val taskJson: TaskJson<T>,
        val assignedToHelper: AssignedToHelper<T>,
) : RemoteRecord(create) {

    companion object {

        const val TASKS = "tasks"
    }

    val instanceRecords = mutableMapOf<ScheduleKey, InstanceRecord<T>>()

    val singleScheduleRecords: MutableMap<String, SingleScheduleRecord<T>> = mutableMapOf()

    val weeklyScheduleRecords: MutableMap<String, WeeklyScheduleRecord<T>> = mutableMapOf()

    val monthlyDayScheduleRecords: MutableMap<String, MonthlyDayScheduleRecord<T>> = mutableMapOf()

    val monthlyWeekScheduleRecords: MutableMap<String, MonthlyWeekScheduleRecord<T>> = mutableMapOf()

    val yearlyScheduleRecords: MutableMap<String, YearlyScheduleRecord<T>> = mutableMapOf()

    val noScheduleOrParentRecords = taskJson.noScheduleOrParent
            .mapValues { NoScheduleOrParentRecord(this, it.value, it.key) }
            .toMutableMap()

    val taskHierarchyRecords = taskJson.taskHierarchies
            .mapValues { NestedTaskHierarchyRecord(it.key, this, it.value) }
            .toMutableMap()

    final override val key get() = projectRecord.childKey + "/" + TASKS + "/" + id

    val rootInstanceKey by lazy { "${projectRecord.projectKey.key}-$id" }

    val projectKey get() = projectRecord.projectKey

    var name by Committer(taskJson::name)

    val startTime get() = taskJson.startTime
    var startTimeOffset by Committer(taskJson::startTimeOffset)

    val taskKey by lazy { projectRecord.getTaskKey(id) }

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
        if (name.isEmpty()) throw MalformedTaskException("taskKey: $key, taskJson: $taskJson")

        for ((key, instanceJson) in taskJson.instances) {
            check(key.isNotEmpty())

            val scheduleKey = InstanceRecord.stringToScheduleKey(projectRecord, key)

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
                            scheduleWrapperBridge
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
                            scheduleWrapperBridge
                    )
                }
                scheduleWrapperBridge.monthlyDayScheduleJson != null -> {
                    check(scheduleWrapperBridge.monthlyWeekScheduleJson == null)
                    check(scheduleWrapperBridge.yearlyScheduleJson == null)

                    monthlyDayScheduleRecords[id] = MonthlyDayScheduleRecord(
                            this,
                            scheduleWrapper,
                            id,
                            scheduleWrapperBridge
                    )
                }
                scheduleWrapperBridge.monthlyWeekScheduleJson != null -> {
                    check(scheduleWrapperBridge.yearlyScheduleJson == null)

                    monthlyWeekScheduleRecords[id] = MonthlyWeekScheduleRecord(
                            this,
                            scheduleWrapper,
                            id,
                            scheduleWrapperBridge
                    )
                }
                else -> {
                    check(scheduleWrapperBridge.yearlyScheduleJson != null)

                    yearlyScheduleRecords[id] = YearlyScheduleRecord(
                            this,
                            scheduleWrapper,
                            id,
                            scheduleWrapperBridge
                    )
                }
            }
        }
    }

    fun newInstanceRecord(instanceJson: InstanceJson, scheduleKey: ScheduleKey): InstanceRecord<T> {
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
            singleScheduleJson: SingleScheduleJson<T>? = null,
            weeklyScheduleJson: WeeklyScheduleJson<T>? = null,
            monthlyDayScheduleJson: MonthlyDayScheduleJson<T>? = null,
            monthlyWeekScheduleJson: MonthlyWeekScheduleJson<T>? = null,
            yearlyScheduleJson: YearlyScheduleJson<T>? = null,
    ): ScheduleWrapper<T>

    fun newSingleScheduleRecord(singleScheduleJson: SingleScheduleJson<T>): SingleScheduleRecord<T> {
        val singleScheduleRecord = SingleScheduleRecord(
                this,
                newScheduleWrapper(singleScheduleJson = singleScheduleJson)
        )

        check(!singleScheduleRecords.containsKey(singleScheduleRecord.id))

        singleScheduleRecords[singleScheduleRecord.id] = singleScheduleRecord
        return singleScheduleRecord
    }

    fun newWeeklyScheduleRecord(weeklyScheduleJson: WeeklyScheduleJson<T>): WeeklyScheduleRecord<T> {
        val weeklyScheduleRecord = WeeklyScheduleRecord(
                this,
                newScheduleWrapper(weeklyScheduleJson = weeklyScheduleJson)
        )

        check(!weeklyScheduleRecords.containsKey(weeklyScheduleRecord.id))

        weeklyScheduleRecords[weeklyScheduleRecord.id] = weeklyScheduleRecord
        return weeklyScheduleRecord
    }

    fun newMonthlyDayScheduleRecord(monthlyDayScheduleJson: MonthlyDayScheduleJson<T>): MonthlyDayScheduleRecord<T> {
        val monthlyDayScheduleRecord = MonthlyDayScheduleRecord(
                this,
                newScheduleWrapper(monthlyDayScheduleJson = monthlyDayScheduleJson)
        )

        check(!monthlyDayScheduleRecords.containsKey(monthlyDayScheduleRecord.id))

        monthlyDayScheduleRecords[monthlyDayScheduleRecord.id] = monthlyDayScheduleRecord
        return monthlyDayScheduleRecord
    }

    fun newMonthlyWeekScheduleRecord(monthlyWeekScheduleJson: MonthlyWeekScheduleJson<T>): MonthlyWeekScheduleRecord<T> {
        val monthlyWeekScheduleRecord = MonthlyWeekScheduleRecord(
                this,
                newScheduleWrapper(monthlyWeekScheduleJson = monthlyWeekScheduleJson)
        )

        check(!monthlyWeekScheduleRecords.containsKey(monthlyWeekScheduleRecord.id))

        monthlyWeekScheduleRecords[monthlyWeekScheduleRecord.id] = monthlyWeekScheduleRecord
        return monthlyWeekScheduleRecord
    }

    fun newYearlyScheduleRecord(yearlyScheduleJson: YearlyScheduleJson<T>): YearlyScheduleRecord<T> {
        val yearlyScheduleRecord = YearlyScheduleRecord(
                this,
                newScheduleWrapper(yearlyScheduleJson = yearlyScheduleJson)
        )

        check(!yearlyScheduleRecords.containsKey(yearlyScheduleRecord.id))

        yearlyScheduleRecords[yearlyScheduleRecord.id] = yearlyScheduleRecord
        return yearlyScheduleRecord
    }

    fun newNoScheduleOrParentRecord(noScheduleOrParentJson: NoScheduleOrParentJson): NoScheduleOrParentRecord<T> {
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

    fun getScheduleRecordId() = projectRecord.getScheduleRecordId(id)
    fun newNoScheduleOrParentRecordId() = projectRecord.newNoScheduleOrParentRecordId(id)

    fun newTaskHierarchyRecordId() = projectRecord.newNestedTaskHierarchyRecordId(id)

    private class MalformedTaskException(message: String) : Exception(message)
}
