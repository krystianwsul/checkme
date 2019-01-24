package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import android.util.Log
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.RemoteTask
import com.krystianwsul.checkme.firebase.json.InstanceJson
import com.krystianwsul.checkme.firebase.json.OldestVisibleJson
import com.krystianwsul.checkme.firebase.json.ScheduleWrapper
import com.krystianwsul.checkme.firebase.json.TaskJson
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.time.Date

class RemoteTaskRecord private constructor(
        create: Boolean,
        val domainFactory: DomainFactory,
        val id: String,
        private val remoteProjectRecord: RemoteProjectRecord,
        private val taskJson: TaskJson) : RemoteRecord(create) {

    companion object {

        const val TASKS = "tasks"
    }

    private val _remoteInstanceRecords = HashMap<ScheduleKey, RemoteInstanceRecord>()

    val remoteSingleScheduleRecords: MutableMap<String, RemoteSingleScheduleRecord> = HashMap()

    val remoteDailyScheduleRecords: MutableMap<String, RemoteDailyScheduleRecord> = HashMap()

    val remoteWeeklyScheduleRecords: MutableMap<String, RemoteWeeklyScheduleRecord> = HashMap()

    val remoteMonthlyDayScheduleRecords: MutableMap<String, RemoteMonthlyDayScheduleRecord> = HashMap()

    val remoteMonthlyWeekScheduleRecords: MutableMap<String, RemoteMonthlyWeekScheduleRecord> = HashMap()

    override val createObject: TaskJson
        // because of duplicate functionality when converting local task
        get() {
            if (update != null)
                taskJson.instances = _remoteInstanceRecords.entries
                        .associateBy({ RemoteInstanceRecord.scheduleKeyToString(domainFactory, remoteProjectRecord.id, it.key) }, { it.value.createObject })
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

    val remoteInstanceRecords: Map<ScheduleKey, RemoteInstanceRecord> get() = _remoteInstanceRecords

    constructor(domainFactory: DomainFactory, id: String, remoteProjectRecord: RemoteProjectRecord, taskJson: TaskJson) : this(
            false,
            domainFactory,
            id,
            remoteProjectRecord,
            taskJson)

    constructor(domainFactory: DomainFactory, remoteProjectRecord: RemoteProjectRecord, taskJson: TaskJson) : this(
            true,
            domainFactory,
            DatabaseWrapper.getTaskRecordId(remoteProjectRecord.id),
            remoteProjectRecord,
            taskJson)

    init {
        check(taskJson.name.isNotEmpty())

        for ((key, instanceJson) in taskJson.instances) {
            check(!TextUtils.isEmpty(key))

            val scheduleKey = RemoteInstanceRecord.stringToScheduleKey(domainFactory, remoteProjectRecord.id, key)

            val remoteInstanceRecord = RemoteInstanceRecord(false, domainFactory, this, instanceJson, scheduleKey)

            _remoteInstanceRecords[scheduleKey] = remoteInstanceRecord
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

    fun setOldestVisible(newOldestVisibleJson: OldestVisibleJson) {
        val oldOldestVisibleJson = oldestVisibleJson

        taskJson.oldestVisible[uuid] = newOldestVisibleJson

        if (oldOldestVisibleJson?.year != newOldestVisibleJson.year)
            addValue("$key/oldestVisible/$uuid/year", newOldestVisibleJson.year)

        if (oldOldestVisibleJson?.month != newOldestVisibleJson.month)
            addValue("$key/oldestVisible/$uuid/month", newOldestVisibleJson.month)

        if (oldOldestVisibleJson?.day != newOldestVisibleJson.day)
            addValue("$key/oldestVisible/$uuid/day", newOldestVisibleJson.day)
    }

    fun setOldestVisibleYear(oldestVisibleYear: Int) {
        if (oldestVisibleYear != taskJson.oldestVisibleYear) {
            taskJson.oldestVisibleYear = oldestVisibleYear
            addValue("$key/oldestVisibleYear", oldestVisibleYear)
        }
    }

    fun setOldestVisibleMonth(oldestVisibleMonth: Int) {
        if (oldestVisibleMonth != taskJson.oldestVisibleMonth) {
            taskJson.oldestVisibleMonth = oldestVisibleMonth
            addValue("$key/oldestVisibleMonth", oldestVisibleMonth)
        }
    }

    fun setOldestVisibleDay(oldestVisibleDay: Int) {
        if (oldestVisibleDay != taskJson.oldestVisibleDay) {
            taskJson.oldestVisibleDay = oldestVisibleDay
            addValue("$key/oldestVisibleDay", oldestVisibleDay)
        }
    }

    override fun getValues(values: MutableMap<String, Any?>) {
        if (delete) {
            Log.e("asdf", "RemoteTaskRecord.getValues deleting " + this)

            check(update != null)

            values[key] = null
            delete = false
        } else {
            if (update == null) {
                Log.e("asdf", "RemoteTaskRecord.getValues creating " + this)

                check(update == null)

                values[key] = createObject
            } else {
                if (update!!.isNotEmpty()) {
                    Log.e("asdf", "RemoteTaskRecord.getValues updating " + this)

                    values.putAll(update!!)
                    update = mutableMapOf()
                }

                for (remoteInstanceRecord in _remoteInstanceRecords.values)
                    remoteInstanceRecord.getValues(values)

                for (remoteSingleScheduleRecord in remoteSingleScheduleRecords.values)
                    remoteSingleScheduleRecord.getValues(values)

                for (remoteDailyScheduleRecord in remoteDailyScheduleRecords.values)
                    remoteDailyScheduleRecord.getValues(values)

                for (remoteWeeklyScheduleRecord in remoteWeeklyScheduleRecords.values)
                    remoteWeeklyScheduleRecord.getValues(values)

                for (remoteMonthlyDayScheduleRecord in remoteMonthlyDayScheduleRecords.values)
                    remoteMonthlyDayScheduleRecord.getValues(values)

                for (remoteMonthlyWeekScheduleRecord in remoteMonthlyWeekScheduleRecords.values)
                    remoteMonthlyWeekScheduleRecord.getValues(values)
            }

            update = mutableMapOf()
        }
    }

    fun newRemoteInstanceRecord(instanceJson: InstanceJson, scheduleKey: ScheduleKey): RemoteInstanceRecord {
        val remoteInstanceRecord = RemoteInstanceRecord(true, domainFactory, this, instanceJson, scheduleKey)
        check(!_remoteInstanceRecords.containsKey(remoteInstanceRecord.scheduleKey))

        _remoteInstanceRecords[remoteInstanceRecord.scheduleKey] = remoteInstanceRecord
        return remoteInstanceRecord
    }

    fun newRemoteSingleScheduleRecord(scheduleWrapper: ScheduleWrapper): RemoteSingleScheduleRecord {
        val remoteSingleScheduleRecord = RemoteSingleScheduleRecord(this, scheduleWrapper)
        check(!remoteSingleScheduleRecords.containsKey(remoteSingleScheduleRecord.id))

        remoteSingleScheduleRecords[remoteSingleScheduleRecord.id] = remoteSingleScheduleRecord
        return remoteSingleScheduleRecord
    }

    fun newRemoteWeeklyScheduleRecord(scheduleWrapper: ScheduleWrapper): RemoteWeeklyScheduleRecord {
        val remoteWeeklyScheduleRecord = RemoteWeeklyScheduleRecord(this, scheduleWrapper)
        check(!remoteWeeklyScheduleRecords.containsKey(remoteWeeklyScheduleRecord.id))

        remoteWeeklyScheduleRecords[remoteWeeklyScheduleRecord.id] = remoteWeeklyScheduleRecord
        return remoteWeeklyScheduleRecord
    }

    fun newRemoteMonthlyDayScheduleRecord(scheduleWrapper: ScheduleWrapper): RemoteMonthlyDayScheduleRecord {
        val remoteMonthlyDayScheduleRecord = RemoteMonthlyDayScheduleRecord(this, scheduleWrapper)
        check(!remoteMonthlyDayScheduleRecords.containsKey(remoteMonthlyDayScheduleRecord.id))

        remoteMonthlyDayScheduleRecords[remoteMonthlyDayScheduleRecord.id] = remoteMonthlyDayScheduleRecord
        return remoteMonthlyDayScheduleRecord
    }

    fun newRemoteMonthlyWeekScheduleRecord(scheduleWrapper: ScheduleWrapper): RemoteMonthlyWeekScheduleRecord {
        val remoteMonthlyWeekScheduleRecord = RemoteMonthlyWeekScheduleRecord(this, scheduleWrapper)
        check(!remoteMonthlyWeekScheduleRecords.containsKey(remoteMonthlyWeekScheduleRecord.id))

        remoteMonthlyWeekScheduleRecords[remoteMonthlyWeekScheduleRecord.id] = remoteMonthlyWeekScheduleRecord
        return remoteMonthlyWeekScheduleRecord
    }
}
