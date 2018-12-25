package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import android.util.Log
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.InstanceJson
import com.krystianwsul.checkme.firebase.json.OldestVisibleJson
import com.krystianwsul.checkme.firebase.json.ScheduleWrapper
import com.krystianwsul.checkme.firebase.json.TaskJson
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.time.Date
import java.util.*

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

    override val createObject: TaskJson// because of duplicate functionality when converting local task
        get() {
            if (!create)
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

    override val key get() = remoteProjectRecord.key + "/" + RemoteProjectRecord.PROJECT_JSON + "/" + TASKS + "/" + id

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

    val endTime get() = taskJson.endTime

    var note
        get() = taskJson.note
        set(note) {
            if (note == taskJson.note)
                return

            taskJson.note = note
            addValue("$key/note", note)
        }

    val oldestVisibleYear get() = taskJson.oldestVisibleYear

    val oldestVisibleMonth get() = taskJson.oldestVisibleMonth

    val oldestVisibleDay get() = taskJson.oldestVisibleDay

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

    fun setEndTime(endTime: Long) {
        check(taskJson.endTime == null)

        if (endTime == taskJson.endTime)
            return

        taskJson.endTime = endTime
        addValue("$key/endTime", endTime)
    }

    private val uuid by lazy { domainFactory.uuid }

    private val oldestVisibleJson get() = taskJson.oldestVisible[uuid]

    fun setOldestVisible(date: Date) {
        val newOldestVisibleJson = OldestVisibleJson(date.year, date.month, date.day)

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
        check(!deleted)
        check(!created)
        check(!updated)

        when {
            delete -> {
                Log.e("asdf", "RemoteTaskRecord.getValues deleting " + this)

                check(!create)
                check(update != null)

                deleted = true
                values[key] = null
            }
            create -> {
                Log.e("asdf", "RemoteTaskRecord.getValues creating " + this)

                check(update == null)

                created = true

                values[key] = createObject
            }
            else -> {
                if (update!!.isNotEmpty()) {
                    Log.e("asdf", "RemoteTaskRecord.getValues updating " + this)

                    updated = true
                    values.putAll(update)
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
