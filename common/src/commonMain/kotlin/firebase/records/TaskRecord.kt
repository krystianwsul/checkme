package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.OldestVisibleJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey

class TaskRecord<T : ProjectType> private constructor(
        create: Boolean,
        val id: String,
        val projectRecord: RemoteProjectRecord<T>,
        private val taskJson: TaskJson
) : RemoteRecord(create) {

    companion object {

        const val TASKS = "tasks"
    }

    val remoteInstanceRecords = mutableMapOf<ScheduleKey, InstanceRecord<T>>()

    val remoteSingleScheduleRecords: MutableMap<String, RemoteSingleScheduleRecord<T>> = HashMap()

    val remoteDailyScheduleRecords: MutableMap<String, RemoteDailyScheduleRecord<T>> = HashMap()

    val remoteWeeklyScheduleRecords: MutableMap<String, RemoteWeeklyScheduleRecord<T>> = HashMap()

    val remoteMonthlyDayScheduleRecords: MutableMap<String, RemoteMonthlyDayScheduleRecord<T>> = HashMap()

    val remoteMonthlyWeekScheduleRecords: MutableMap<String, RemoteMonthlyWeekScheduleRecord<T>> = HashMap()

    override val createObject: TaskJson
        // because of duplicate functionality when converting local task
        get() {
            if (update != null)
                taskJson.instances = remoteInstanceRecords.entries
                        .associateBy({ InstanceRecord.scheduleKeyToString(it.key) }, { it.value.createObject })
                        .toMutableMap()

            val scheduleWrappers = HashMap<String, ScheduleWrapper>()

            for (remoteSingleScheduleRecord in remoteSingleScheduleRecords.values)
                scheduleWrappers[remoteSingleScheduleRecord.id] = remoteSingleScheduleRecord.createObject

            for (remoteDailyScheduleRecord in remoteDailyScheduleRecords.values)
                scheduleWrappers[remoteDailyScheduleRecord.id] = remoteDailyScheduleRecord.createObject

            for (remoteWeeklyScheduleRecord in remoteWeeklyScheduleRecords.values)
                scheduleWrappers[remoteWeeklyScheduleRecord.id] = remoteWeeklyScheduleRecord.createObject

            for (remoteMonthlyDayScheduleRecord in remoteMonthlyDayScheduleRecords.values)
                scheduleWrappers[remoteMonthlyDayScheduleRecord.id] = remoteMonthlyDayScheduleRecord.createObject

            for (remoteMonthlyWeekScheduleRecord in remoteMonthlyWeekScheduleRecords.values)
                scheduleWrappers[remoteMonthlyWeekScheduleRecord.id] = remoteMonthlyWeekScheduleRecord.createObject

            taskJson.schedules = scheduleWrappers

            return taskJson
        }

    override val key get() = projectRecord.childKey + "/" + TASKS + "/" + id

    val projectId get() = projectRecord.projectKey

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
        get() = taskJson.oldestVisible
                .values
                .map { it.toDate() }
                .min()

    var image by Committer(taskJson::image)

    constructor(
            id: String,
            remoteProjectRecord: RemoteProjectRecord<T>,
            taskJson: TaskJson
    ) : this(
            false,
            id,
            remoteProjectRecord,
            taskJson
    )

    constructor(
            remoteProjectRecord: RemoteProjectRecord<T>,
            taskJson: TaskJson
    ) : this(
            true,
            remoteProjectRecord.getTaskRecordId(),
            remoteProjectRecord,
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

            remoteInstanceRecords[scheduleKey] = remoteInstanceRecord
        }

        for ((id, scheduleWrapper) in taskJson.schedules) {
            check(id.isNotEmpty())

            when {
                scheduleWrapper.singleScheduleJson != null -> {
                    check(scheduleWrapper.dailyScheduleJson == null)
                    check(scheduleWrapper.weeklyScheduleJson == null)
                    check(scheduleWrapper.monthlyDayScheduleJson == null)
                    check(scheduleWrapper.monthlyWeekScheduleJson == null)

                    remoteSingleScheduleRecords[id] = RemoteSingleScheduleRecord(id, this, scheduleWrapper)
                }
                scheduleWrapper.dailyScheduleJson != null -> {
                    check(scheduleWrapper.weeklyScheduleJson == null)
                    check(scheduleWrapper.monthlyDayScheduleJson == null)
                    check(scheduleWrapper.monthlyWeekScheduleJson == null)

                    remoteDailyScheduleRecords[id] = RemoteDailyScheduleRecord(id, this, scheduleWrapper)
                }
                scheduleWrapper.weeklyScheduleJson != null -> {
                    check(scheduleWrapper.monthlyDayScheduleJson == null)
                    check(scheduleWrapper.monthlyWeekScheduleJson == null)

                    remoteWeeklyScheduleRecords[id] = RemoteWeeklyScheduleRecord(id, this, scheduleWrapper)
                }
                scheduleWrapper.monthlyDayScheduleJson != null -> {
                    check(scheduleWrapper.monthlyWeekScheduleJson == null)

                    remoteMonthlyDayScheduleRecords[id] = RemoteMonthlyDayScheduleRecord(id, this, scheduleWrapper)
                }
                else -> {
                    check(scheduleWrapper.monthlyWeekScheduleJson != null)

                    remoteMonthlyWeekScheduleRecords[id] = RemoteMonthlyWeekScheduleRecord(id, this, scheduleWrapper)
                }
            }
        }
    }

    private fun getOldestVisibleJson(uuid: String) = taskJson.oldestVisible[uuid]

    override val children
        get() = remoteInstanceRecords.values +
                remoteSingleScheduleRecords.values +
                remoteDailyScheduleRecords.values +
                remoteWeeklyScheduleRecords.values +
                remoteMonthlyDayScheduleRecords.values +
                remoteMonthlyWeekScheduleRecords.values

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

    fun newRemoteInstanceRecord(
            instanceJson: InstanceJson,
            scheduleKey: ScheduleKey,
            customTimeId: CustomTimeId<T>?
    ): InstanceRecord<T> {
        val firebaseKey = InstanceRecord.scheduleKeyToString(scheduleKey)

        val remoteInstanceRecord = ProjectInstanceRecord(
                true,
                this,
                instanceJson,
                scheduleKey,
                firebaseKey,
                customTimeId
        )

        check(!remoteInstanceRecords.containsKey(remoteInstanceRecord.scheduleKey))

        remoteInstanceRecords[remoteInstanceRecord.scheduleKey] = remoteInstanceRecord
        return remoteInstanceRecord
    }

    fun newRemoteSingleScheduleRecord(scheduleWrapper: ScheduleWrapper): RemoteSingleScheduleRecord<T> {
        val remoteSingleScheduleRecord = RemoteSingleScheduleRecord(this, scheduleWrapper)
        check(!remoteSingleScheduleRecords.containsKey(remoteSingleScheduleRecord.id))

        remoteSingleScheduleRecords[remoteSingleScheduleRecord.id] = remoteSingleScheduleRecord
        return remoteSingleScheduleRecord
    }

    fun newRemoteWeeklyScheduleRecord(scheduleWrapper: ScheduleWrapper): RemoteWeeklyScheduleRecord<T> {
        val remoteWeeklyScheduleRecord = RemoteWeeklyScheduleRecord(this, scheduleWrapper)
        check(!remoteWeeklyScheduleRecords.containsKey(remoteWeeklyScheduleRecord.id))

        remoteWeeklyScheduleRecords[remoteWeeklyScheduleRecord.id] = remoteWeeklyScheduleRecord
        return remoteWeeklyScheduleRecord
    }

    fun newRemoteMonthlyDayScheduleRecord(scheduleWrapper: ScheduleWrapper): RemoteMonthlyDayScheduleRecord<T> {
        val remoteMonthlyDayScheduleRecord = RemoteMonthlyDayScheduleRecord(this, scheduleWrapper)
        check(!remoteMonthlyDayScheduleRecords.containsKey(remoteMonthlyDayScheduleRecord.id))

        remoteMonthlyDayScheduleRecords[remoteMonthlyDayScheduleRecord.id] = remoteMonthlyDayScheduleRecord
        return remoteMonthlyDayScheduleRecord
    }

    fun newRemoteMonthlyWeekScheduleRecord(scheduleWrapper: ScheduleWrapper): RemoteMonthlyWeekScheduleRecord<T> {
        val remoteMonthlyWeekScheduleRecord = RemoteMonthlyWeekScheduleRecord(this, scheduleWrapper)
        check(!remoteMonthlyWeekScheduleRecords.containsKey(remoteMonthlyWeekScheduleRecord.id))

        remoteMonthlyWeekScheduleRecords[remoteMonthlyWeekScheduleRecord.id] = remoteMonthlyWeekScheduleRecord
        return remoteMonthlyWeekScheduleRecord
    }

    override fun deleteFromParent() = check(projectRecord.remoteTaskRecords.remove(id) == this)

    fun getScheduleRecordId() = projectRecord.getScheduleRecordId(id)

    fun getcustomTimeId(id: String) = projectRecord.getCustomTimeId(id)
    fun getRemoteCustomTimeKey(id: String) = projectRecord.getRemoteCustomTimeKey(id)

    private class MalformedTaskException(message: String) : Exception(message)

    class OnlyVisibilityPresentException(message: String) : Exception(message)
}
