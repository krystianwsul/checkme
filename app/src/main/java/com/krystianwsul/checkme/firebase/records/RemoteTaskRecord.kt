package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.checkme.firebase.RemoteTask
import com.krystianwsul.checkme.firebase.json.InstanceJson
import com.krystianwsul.checkme.firebase.json.OldestVisibleJson
import com.krystianwsul.checkme.firebase.json.ScheduleWrapper
import com.krystianwsul.checkme.firebase.json.TaskJson
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.time.Date

class RemoteTaskRecord<T : RemoteCustomTimeId> private constructor(
        create: Boolean,
        val domainFactory: DomainFactory,
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
            check(!TextUtils.isEmpty(name))

            if (name == taskJson.name)
                return

            taskJson.name = name
            addValue("$key/name", name)
        }

    val startTime get() = taskJson.startTime

    var endTime
        get() = taskJson.endTime
        set(value) {
            if (value == taskJson.endTime)
                return

            taskJson.endTime = value
            addValue("$key/endTime", value)
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
                            MyCrashlytics.logException(RemoteTask.MissingDayException("projectId: $projectId, taskId: $id, oldestVisibleYear: ${taskJson.oldestVisibleYear}, oldestVisibleMonth: ${taskJson.oldestVisibleMonth}, oldestVisibleDay: ${taskJson.oldestVisibleDay}"))
                    }
                }
                .min()

    constructor(domainFactory: DomainFactory, id: String, remoteProjectRecord: RemoteProjectRecord<T>, taskJson: TaskJson) : this(
            false,
            domainFactory,
            id,
            remoteProjectRecord,
            taskJson)

    constructor(domainFactory: DomainFactory, remoteProjectRecord: RemoteProjectRecord<T>, taskJson: TaskJson) : this(
            true,
            domainFactory,
            remoteProjectRecord.getTaskRecordId(),
            remoteProjectRecord,
            taskJson)

    init {
        if (taskJson.name.isEmpty())
            MyCrashlytics.logException(MissingNameException("taskKey: $key"))

        for ((key, instanceJson) in taskJson.instances) {
            check(!TextUtils.isEmpty(key))

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
            check(!TextUtils.isEmpty(id))

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

    private val uuid by lazy { domainFactory.uuid }

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

    fun newRemoteInstanceRecord(remoteProject: RemoteProject<T>, instanceJson: InstanceJson, scheduleKey: ScheduleKey): RemoteInstanceRecord<T> {
        val remoteCustomTimeId = scheduleKey.scheduleTimePair
                .customTimeKey
                ?.let { remoteProject.getRemoteCustomTime(it.remoteCustomTimeId).id }

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
}
