package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.OldestVisibleJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.ScheduleKey

class RemoteTaskRecord<T : RemoteCustomTimeId> private constructor(
        create: Boolean,
        private val uuid: String,
        val id: String,
        private val remoteProjectRecord: RemoteProjectRecord<T>,
        private val taskJson: TaskJson) : RemoteRecord(create) {

    companion object {

        const val TASKS = "tasks"
    }

    val remoteInstanceRecords = mutableMapOf<ScheduleKey, RemoteInstanceRecord<T>>()

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
                        .associateBy({ RemoteInstanceRecord.scheduleKeyToString(it.key) }, { it.value.createObject })
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

    override val key get() = remoteProjectRecord.childKey + "/" + TASKS + "/" + id

    val projectId get() = remoteProjectRecord.id

    var name
        get() = taskJson.name
        set(name) {
            check(name.isNotEmpty())

            if (name == taskJson.name)
                return

            taskJson.name = name
            addValue("$key/name", name)
        }

    val startTime get() = taskJson.startTime

    var endData
        get() = taskJson.endData ?: taskJson.endTime?.let { TaskJson.EndData(it, false) }
        set(value) {
            if (value == taskJson.endData)
                return

            taskJson.endData = value
            addValue("$key/endData", value)

            taskJson.endTime = value?.time
            addValue("$key/endData", value)
        }

    var note
        get() = taskJson.note
        set(note) {
            if (note == taskJson.note)
                return

            taskJson.note = note
            addValue("$key/note", note)
        }

    val oldestVisible
        get() = taskJson.oldestVisible
                .values
                .map { it.toDate() }
                .toMutableList()
                .apply {
                    if (taskJson.oldestVisibleYear != null && taskJson.oldestVisibleMonth != null && taskJson.oldestVisibleDay != null) {
                        add(Date(taskJson.oldestVisibleYear!!, taskJson.oldestVisibleMonth!!, taskJson.oldestVisibleDay!!))
                    } else {
                        if (taskJson.oldestVisibleYear != null || taskJson.oldestVisibleMonth != null || taskJson.oldestVisibleDay != null)
                            ErrorLogger.instance.logException(MissingDayException("projectId: $projectId, taskId: $id, oldestVisibleYear: ${taskJson.oldestVisibleYear}, oldestVisibleMonth: ${taskJson.oldestVisibleMonth}, oldestVisibleDay: ${taskJson.oldestVisibleDay}"))
                    }
                }
                .min()

    var image
        get() = taskJson.image
        set(value) {
            if (value == taskJson.image)
                return

            taskJson.image = value
            addValue("$key/image", value)
        }

    constructor(
            id: String,
            uuid: String,
            remoteProjectRecord: RemoteProjectRecord<T>,
            taskJson: TaskJson
    ) : this(
            false,
            uuid,
            id,
            remoteProjectRecord,
            taskJson
    )

    constructor(
            uuid: String,
            remoteProjectRecord: RemoteProjectRecord<T>,
            taskJson: TaskJson
    ) : this(
            true,
            uuid,
            remoteProjectRecord.getTaskRecordId(),
            remoteProjectRecord,
            taskJson
    )

    init {
        if (taskJson.name.isEmpty())
            ErrorLogger.instance.logException(MissingNameException("taskKey: $key"))

        for ((key, instanceJson) in taskJson.instances) {
            check(key.isNotEmpty())

            val (scheduleKey, remoteCustomTimeId) = RemoteInstanceRecord.stringToScheduleKey(remoteProjectRecord, key)

            val remoteInstanceRecord = RemoteInstanceRecord(
                    create,
                    this,
                    instanceJson,
                    scheduleKey,
                    key,
                    remoteCustomTimeId)

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

    private val oldestVisibleJson get() = taskJson.oldestVisible[uuid]

    override val children
        get() = remoteInstanceRecords.values +
                remoteSingleScheduleRecords.values +
                remoteDailyScheduleRecords.values +
                remoteWeeklyScheduleRecords.values +
                remoteMonthlyDayScheduleRecords.values +
                remoteMonthlyWeekScheduleRecords.values

    fun setOldestVisible(newOldestVisibleJson: OldestVisibleJson) {
        val oldOldestVisibleJson = oldestVisibleJson

        taskJson.oldestVisible[uuid] = newOldestVisibleJson

        if (oldOldestVisibleJson?.date != newOldestVisibleJson.date)
            addValue("$key/oldestVisible/$uuid/date", newOldestVisibleJson.date)

        if (oldOldestVisibleJson?.year != newOldestVisibleJson.year)
            addValue("$key/oldestVisible/$uuid/year", newOldestVisibleJson.year)

        if (oldOldestVisibleJson?.month != newOldestVisibleJson.month)
            addValue("$key/oldestVisible/$uuid/month", newOldestVisibleJson.month)

        if (oldOldestVisibleJson?.day != newOldestVisibleJson.day)
            addValue("$key/oldestVisible/$uuid/day", newOldestVisibleJson.day)

        if (newOldestVisibleJson.year != taskJson.oldestVisibleYear) {
            taskJson.oldestVisibleYear = newOldestVisibleJson.year
            addValue("$key/oldestVisibleYear", newOldestVisibleJson.year)
        }

        if (newOldestVisibleJson.month != taskJson.oldestVisibleMonth) {
            taskJson.oldestVisibleMonth = newOldestVisibleJson.month
            addValue("$key/oldestVisibleMonth", newOldestVisibleJson.month)
        }

        if (newOldestVisibleJson.day != taskJson.oldestVisibleDay) {
            taskJson.oldestVisibleDay = newOldestVisibleJson.day
            addValue("$key/oldestVisibleDay", newOldestVisibleJson.day)
        }
    }

    fun newRemoteInstanceRecord(
            instanceJson: InstanceJson,
            scheduleKey: ScheduleKey,
            remoteCustomTimeId: T?
    ): RemoteInstanceRecord<T> {
        val firebaseKey = RemoteInstanceRecord.scheduleKeyToString(scheduleKey)

        val remoteInstanceRecord = RemoteInstanceRecord(true, this, instanceJson, scheduleKey, firebaseKey, remoteCustomTimeId)
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

    override fun deleteFromParent() = check(remoteProjectRecord.remoteTaskRecords.remove(id) == this)

    fun getScheduleRecordId() = remoteProjectRecord.getScheduleRecordId(id)

    fun getRemoteCustomTimeId(id: String) = remoteProjectRecord.getRemoteCustomTimeId(id)

    private class MissingNameException(message: String) : Exception(message)

    private class MissingDayException(message: String) : Exception(message)
}
