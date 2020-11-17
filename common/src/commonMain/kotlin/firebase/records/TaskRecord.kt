package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey

abstract class TaskRecord<T : ProjectType> protected constructor(
        create: Boolean,
        val id: String,
        val projectRecord: ProjectRecord<T>,
        private val taskJson: TaskJson,
) : RemoteRecord(create) {

    companion object {

        const val TASKS = "tasks"
    }

    val instanceRecords = mutableMapOf<ScheduleKey, ProjectInstanceRecord<T>>()

    val singleScheduleRecords: MutableMap<String, SingleScheduleRecord<T>> = mutableMapOf()

    val weeklyScheduleRecords: MutableMap<String, WeeklyScheduleRecord<T>> = mutableMapOf()

    val monthlyDayScheduleRecords: MutableMap<String, MonthlyDayScheduleRecord<T>> = mutableMapOf()

    val monthlyWeekScheduleRecords: MutableMap<String, MonthlyWeekScheduleRecord<T>> = mutableMapOf()

    val yearlyScheduleRecords: MutableMap<String, YearlyScheduleRecord<T>> = mutableMapOf()

    val noScheduleOrParentRecords = taskJson.noScheduleOrParent
            .mapValues { NoScheduleOrParentRecord(this, it.value, it.key) }
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

    abstract var assignedTo: Set<String>

    final override val children
        get() = instanceRecords.values +
                singleScheduleRecords.values +
                weeklyScheduleRecords.values +
                monthlyDayScheduleRecords.values +
                monthlyWeekScheduleRecords.values +
                yearlyScheduleRecords.values +
                noScheduleOrParentRecords.values

    init {
        if (name.isEmpty())
            throw MalformedTaskException("taskKey: $key, taskJson: $taskJson")

        for ((key, instanceJson) in taskJson.instances) {
            check(key.isNotEmpty())

            val (scheduleKey, customTimeId) = InstanceRecord.stringToScheduleKey(projectRecord, key)

            val remoteInstanceRecord = ProjectInstanceRecord(
                    create,
                    this,
                    instanceJson,
                    scheduleKey,
                    key,
                    customTimeId
            )

            instanceRecords[scheduleKey] = remoteInstanceRecord
        }

        for ((id, scheduleWrapper) in taskJson.schedules) {
            check(id.isNotEmpty())

            when {
                scheduleWrapper.singleScheduleJson != null -> {
                    check(scheduleWrapper.weeklyScheduleJson == null)
                    check(scheduleWrapper.monthlyDayScheduleJson == null)
                    check(scheduleWrapper.monthlyWeekScheduleJson == null)
                    check(scheduleWrapper.yearlyScheduleJson == null)

                    singleScheduleRecords[id] = SingleScheduleRecord(this, scheduleWrapper, id)
                }
                scheduleWrapper.weeklyScheduleJson != null -> {
                    check(scheduleWrapper.monthlyDayScheduleJson == null)
                    check(scheduleWrapper.monthlyWeekScheduleJson == null)
                    check(scheduleWrapper.yearlyScheduleJson == null)

                    weeklyScheduleRecords[id] = WeeklyScheduleRecord(this, scheduleWrapper, id)
                }
                scheduleWrapper.monthlyDayScheduleJson != null -> {
                    check(scheduleWrapper.monthlyWeekScheduleJson == null)
                    check(scheduleWrapper.yearlyScheduleJson == null)

                    monthlyDayScheduleRecords[id] = MonthlyDayScheduleRecord(this, scheduleWrapper, id)
                }
                scheduleWrapper.monthlyWeekScheduleJson != null -> {
                    check(scheduleWrapper.yearlyScheduleJson == null)

                    monthlyWeekScheduleRecords[id] = MonthlyWeekScheduleRecord(this, scheduleWrapper, id)
                }
                else -> {
                    check(scheduleWrapper.yearlyScheduleJson != null)

                    yearlyScheduleRecords[id] = YearlyScheduleRecord(this, scheduleWrapper, id)
                }
            }
        }
    }

    fun newInstanceRecord(
            instanceJson: InstanceJson,
            scheduleKey: ScheduleKey,
            customTimeId: CustomTimeId<T>?
    ): InstanceRecord<T> {
        val firebaseKey = InstanceRecord.scheduleKeyToString(scheduleKey)

        val projectInstanceRecord = ProjectInstanceRecord(
                true,
                this,
                instanceJson,
                scheduleKey,
                firebaseKey,
                customTimeId
        )

        check(!instanceRecords.containsKey(projectInstanceRecord.scheduleKey))

        instanceRecords[projectInstanceRecord.scheduleKey] = projectInstanceRecord
        return projectInstanceRecord
    }

    fun newSingleScheduleRecord(singleScheduleJson: SingleScheduleJson): SingleScheduleRecord<T> {
        val singleScheduleRecord = SingleScheduleRecord(
                this,
                ScheduleWrapper(singleScheduleJson = singleScheduleJson)
        )

        check(!singleScheduleRecords.containsKey(singleScheduleRecord.id))

        singleScheduleRecords[singleScheduleRecord.id] = singleScheduleRecord
        return singleScheduleRecord
    }

    fun newWeeklyScheduleRecord(weeklyScheduleJson: WeeklyScheduleJson): WeeklyScheduleRecord<T> {
        val weeklyScheduleRecord = WeeklyScheduleRecord(
                this,
                ScheduleWrapper(weeklyScheduleJson = weeklyScheduleJson)
        )

        check(!weeklyScheduleRecords.containsKey(weeklyScheduleRecord.id))

        weeklyScheduleRecords[weeklyScheduleRecord.id] = weeklyScheduleRecord
        return weeklyScheduleRecord
    }

    fun newMonthlyDayScheduleRecord(monthlyDayScheduleJson: MonthlyDayScheduleJson): MonthlyDayScheduleRecord<T> {
        val monthlyDayScheduleRecord = MonthlyDayScheduleRecord(
                this,
                ScheduleWrapper(monthlyDayScheduleJson = monthlyDayScheduleJson)
        )

        check(!monthlyDayScheduleRecords.containsKey(monthlyDayScheduleRecord.id))

        monthlyDayScheduleRecords[monthlyDayScheduleRecord.id] = monthlyDayScheduleRecord
        return monthlyDayScheduleRecord
    }

    fun newMonthlyWeekScheduleRecord(monthlyWeekScheduleJson: MonthlyWeekScheduleJson): MonthlyWeekScheduleRecord<T> {
        val monthlyWeekScheduleRecord = MonthlyWeekScheduleRecord(
                this,
                ScheduleWrapper(monthlyWeekScheduleJson = monthlyWeekScheduleJson)
        )

        check(!monthlyWeekScheduleRecords.containsKey(monthlyWeekScheduleRecord.id))

        monthlyWeekScheduleRecords[monthlyWeekScheduleRecord.id] = monthlyWeekScheduleRecord
        return monthlyWeekScheduleRecord
    }

    fun newYearlyScheduleRecord(yearlyScheduleJson: YearlyScheduleJson): YearlyScheduleRecord<T> {
        val yearlyScheduleRecord = YearlyScheduleRecord(
                this,
                ScheduleWrapper(yearlyScheduleJson = yearlyScheduleJson)
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

    fun getScheduleRecordId() = projectRecord.getScheduleRecordId(id)

    fun getCustomTimeId(id: String) = projectRecord.getCustomTimeId(id)
    fun getCustomTimeKey(id: String) = projectRecord.getCustomTimeKey(id)

    fun newNoScheduleOrParentRecordId() = projectRecord.newNoScheduleOrParentRecordId(id)

    private class MalformedTaskException(message: String) : Exception(message)
}
