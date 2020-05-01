package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.OldestVisibleJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey

class TaskRecord<T : ProjectType> private constructor(
        create: Boolean,
        val id: String,
        val projectRecord: ProjectRecord<T>,
        private val taskJson: TaskJson
) : RemoteRecord(create) {

    companion object {

        const val TASKS = "tasks"
    }

    val instanceRecords = mutableMapOf<ScheduleKey, ProjectInstanceRecord<T>>()

    val singleScheduleRecords: MutableMap<String, SingleScheduleRecord<T>> = HashMap()

    val weeklyScheduleRecords: MutableMap<String, WeeklyScheduleRecord<T>> = HashMap()

    val monthlyDayScheduleRecords: MutableMap<String, MonthlyDayScheduleRecord<T>> = HashMap()

    val monthlyWeekScheduleRecords: MutableMap<String, MonthlyWeekScheduleRecord<T>> = HashMap()

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

            taskJson.schedules = scheduleWrappers

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

    val oldestVisible
        get() = taskJson.oldestVisibleServer
                ?.let { Date.fromJson(it) }
                ?: taskJson.oldestVisible
                        .values
                        .map { it.toDate() }
                        .min()

    var image by Committer(taskJson::image)

    var oldestVisibleServer by Committer(taskJson::oldestVisibleServer)

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
        val malformedOldestVisible = taskJson.oldestVisible.filter {
            try {
                it.value.toDate()
                false
            } catch (exception: Exception) {
                true
            }
        }

        if (malformedOldestVisible.isNotEmpty() || name.isEmpty()) {
            if (taskJson == TaskJson(oldestVisible = taskJson.oldestVisible)) {
                throw OnlyVisibilityPresentException("taskKey: $key")
            } else {
                val malformedTaskException = MalformedTaskException("taskKey: $key, taskJson: $taskJson")
                ErrorLogger.instance.logException(malformedTaskException)
            }
        }

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

                    singleScheduleRecords[id] = SingleScheduleRecord(id, this, scheduleWrapper)
                }
                scheduleWrapper.weeklyScheduleJson != null -> {
                    check(scheduleWrapper.monthlyDayScheduleJson == null)
                    check(scheduleWrapper.monthlyWeekScheduleJson == null)

                    weeklyScheduleRecords[id] = WeeklyScheduleRecord(id, this, scheduleWrapper)
                }
                scheduleWrapper.monthlyDayScheduleJson != null -> {
                    check(scheduleWrapper.monthlyWeekScheduleJson == null)

                    monthlyDayScheduleRecords[id] = MonthlyDayScheduleRecord(id, this, scheduleWrapper)
                }
                else -> {
                    check(scheduleWrapper.monthlyWeekScheduleJson != null)

                    monthlyWeekScheduleRecords[id] = MonthlyWeekScheduleRecord(id, this, scheduleWrapper)
                }
            }
        }
    }

    private fun getOldestVisibleJson(uuid: String) = taskJson.oldestVisible[uuid]

    override val children
        get() = instanceRecords.values +
                singleScheduleRecords.values +
                weeklyScheduleRecords.values +
                monthlyDayScheduleRecords.values +
                monthlyWeekScheduleRecords.values

    fun setOldestVisible(uuid: String, newOldestVisibleJson: OldestVisibleJson) {
        val oldOldestVisibleJson = getOldestVisibleJson(uuid)

        taskJson.oldestVisible[uuid] = newOldestVisibleJson

        if (oldOldestVisibleJson?.date != newOldestVisibleJson.date)
            addValue("$key/oldestVisible/$uuid/date", newOldestVisibleJson.date)

        if (oldOldestVisibleJson?.year != newOldestVisibleJson.year)
            addValue("$key/oldestVisible/$uuid/year", newOldestVisibleJson.year)

        if (oldOldestVisibleJson?.month != newOldestVisibleJson.month)
            addValue("$key/oldestVisible/$uuid/month", newOldestVisibleJson.month)

        if (oldOldestVisibleJson?.day != newOldestVisibleJson.day)
            addValue("$key/oldestVisible/$uuid/day", newOldestVisibleJson.day)
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

    fun newSingleScheduleRecord(scheduleWrapper: ScheduleWrapper): SingleScheduleRecord<T> {
        val singleScheduleRecord = SingleScheduleRecord(this, scheduleWrapper)
        check(!singleScheduleRecords.containsKey(singleScheduleRecord.id))

        singleScheduleRecords[singleScheduleRecord.id] = singleScheduleRecord
        return singleScheduleRecord
    }

    fun newWeeklyScheduleRecord(scheduleWrapper: ScheduleWrapper): WeeklyScheduleRecord<T> {
        val weeklyScheduleRecord = WeeklyScheduleRecord(this, scheduleWrapper)
        check(!weeklyScheduleRecords.containsKey(weeklyScheduleRecord.id))

        weeklyScheduleRecords[weeklyScheduleRecord.id] = weeklyScheduleRecord
        return weeklyScheduleRecord
    }

    fun newMonthlyDayScheduleRecord(scheduleWrapper: ScheduleWrapper): MonthlyDayScheduleRecord<T> {
        val monthlyDayScheduleRecord = MonthlyDayScheduleRecord(this, scheduleWrapper)
        check(!monthlyDayScheduleRecords.containsKey(monthlyDayScheduleRecord.id))

        monthlyDayScheduleRecords[monthlyDayScheduleRecord.id] = monthlyDayScheduleRecord
        return monthlyDayScheduleRecord
    }

    fun newMonthlyWeekScheduleRecord(scheduleWrapper: ScheduleWrapper): MonthlyWeekScheduleRecord<T> {
        val monthlyWeekScheduleRecord = MonthlyWeekScheduleRecord(this, scheduleWrapper)
        check(!monthlyWeekScheduleRecords.containsKey(monthlyWeekScheduleRecord.id))

        monthlyWeekScheduleRecords[monthlyWeekScheduleRecord.id] = monthlyWeekScheduleRecord
        return monthlyWeekScheduleRecord
    }

    override fun deleteFromParent() = check(projectRecord.taskRecords.remove(id) == this)

    fun getScheduleRecordId() = projectRecord.getScheduleRecordId(id)

    fun getCustomTimeId(id: String) = projectRecord.getCustomTimeId(id)
    fun getCustomTimeKey(id: String) = projectRecord.getCustomTimeKey(id)

    private class MalformedTaskException(message: String) : Exception(message)

    class OnlyVisibilityPresentException(message: String) : Exception(message)
}
