package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey

class TaskRecord<T : ProjectType> private constructor(
        create: Boolean,
        val id: String,
        val projectRecord: ProjectRecord<T>,
        val taskJson: TaskJson
) : RemoteRecord(create) {

    companion object {

        const val TASKS = "tasks"

        private fun <T : ProjectType> dateTimeStringToSchedulePair(
                projectRecord: ProjectRecord<T>,
                dateTimeString: String
        ): Pair<ScheduleKey, CustomTimeId<T>?> {
            val (dateString, timeString) = dateTimeString.split(':')

            return RootInstanceRecord.dateTimeStringsToSchedulePair(projectRecord, dateString, timeString)
        }

        fun scheduleKeyToString(scheduleKey: ScheduleKey) = scheduleKey.let {
            InstanceRecord.run {
                scheduleKeyToDateString(it, true) + ":" + scheduleKeyToTimeString(it, true)
            }
        }
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

    override val createObject: TaskJson // because of duplicate functionality when converting local task
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

    override val key get() = projectRecord.childKey + "/" + TASKS + "/" + id

    val rootInstanceKey by lazy { "${projectRecord.projectKey.key}-$id" }

    val projectKey get() = projectRecord.projectKey

    var name by Committer(taskJson::name)

    val startTime get() = taskJson.startTime

    val taskKey by lazy { projectRecord.getTaskKey(id) }

    var endData
        get() = taskJson.endData ?: taskJson.endTime?.let { TaskJson.EndData(it, false) }
        set(value) {
            if (value == taskJson.endData)
                return

            setProperty(taskJson::endData, value)
            setProperty(taskJson::endTime, value?.time)
        }

    var note by Committer(taskJson::note)

    var image by Committer(taskJson::image)

    var ordinal by Committer(taskJson::ordinal)

    constructor(
        id: String,
        projectRecord: ProjectRecord<T>,
        taskJson: TaskJson
    ) : this(
        false,
        id,
        projectRecord,
        taskJson
    )

    constructor(
            projectRecord: ProjectRecord<T>,
            taskJson: TaskJson
    ) : this(
            true,
            projectRecord.getTaskRecordId(),
            projectRecord,
            taskJson
    )

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
                    customTimeId)

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

    override val children
        get() = instanceRecords.values +
                singleScheduleRecords.values +
                weeklyScheduleRecords.values +
                monthlyDayScheduleRecords.values +
                monthlyWeekScheduleRecords.values +
                yearlyScheduleRecords.values +
                noScheduleOrParentRecords.values

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

    override fun deleteFromParent() = check(projectRecord.taskRecords.remove(id) == this)

    fun getScheduleRecordId() = projectRecord.getScheduleRecordId(id)

    fun getCustomTimeId(id: String) = projectRecord.getCustomTimeId(id)
    fun getCustomTimeKey(id: String) = projectRecord.getCustomTimeKey(id)

    fun newNoScheduleOrParentRecordId() = projectRecord.newNoScheduleOrParentRecordId(id)

    private class MalformedTaskException(message: String) : Exception(message)
}
