package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import android.util.Log
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.InstanceJson
import com.krystianwsul.checkme.firebase.json.ScheduleWrapper
import com.krystianwsul.checkme.firebase.json.TaskJson
import com.krystianwsul.checkme.utils.ScheduleKey
import junit.framework.Assert
import java.util.*

class RemoteTaskRecord : RemoteRecord {

    companion object {

        const val TASKS = "tasks"
    }

    private val domainFactory: DomainFactory

    val id: String

    private val remoteProjectRecord: RemoteProjectRecord

    private val taskJson: TaskJson

    private val _remoteInstanceRecords = HashMap<ScheduleKey, RemoteInstanceRecord>()

    val remoteSingleScheduleRecords: MutableMap<String, RemoteSingleScheduleRecord> = HashMap()

    val remoteDailyScheduleRecords: MutableMap<String, RemoteDailyScheduleRecord> = HashMap()

    val remoteWeeklyScheduleRecords: MutableMap<String, RemoteWeeklyScheduleRecord> = HashMap()

    val remoteMonthlyDayScheduleRecords: MutableMap<String, RemoteMonthlyDayScheduleRecord> = HashMap()

    val remoteMonthlyWeekScheduleRecords: MutableMap<String, RemoteMonthlyWeekScheduleRecord> = HashMap()

    override val createObject: TaskJson// because of duplicate functionality when converting local task
        get() {
            if (!create)
                taskJson.instances = _remoteInstanceRecords.entries.associateBy({ RemoteInstanceRecord.scheduleKeyToString(domainFactory, remoteProjectRecord.id, it.key) }, { it.value.createObject })

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
            Assert.assertTrue(!TextUtils.isEmpty(name))

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

    constructor(domainFactory: DomainFactory, id: String, remoteProjectRecord: RemoteProjectRecord, taskJson: TaskJson) : super(false) {
        this.domainFactory = domainFactory
        this.id = id
        this.remoteProjectRecord = remoteProjectRecord
        this.taskJson = taskJson

        initialize()
    }

    constructor(domainFactory: DomainFactory, remoteProjectRecord: RemoteProjectRecord, taskJson: TaskJson) : super(true) {
        this.domainFactory = domainFactory
        id = DatabaseWrapper.getTaskRecordId(remoteProjectRecord.id)
        this.remoteProjectRecord = remoteProjectRecord
        this.taskJson = taskJson

        initialize()
    }

    private fun initialize() {
        for ((key, instanceJson) in taskJson.instances) {
            Assert.assertTrue(!TextUtils.isEmpty(key))

            val scheduleKey = RemoteInstanceRecord.stringToScheduleKey(domainFactory, remoteProjectRecord.id, key)

            Assert.assertTrue(instanceJson != null)

            val remoteInstanceRecord = RemoteInstanceRecord(false, domainFactory, this, instanceJson!!, scheduleKey)

            _remoteInstanceRecords[scheduleKey] = remoteInstanceRecord
        }

        for ((id, scheduleWrapper) in taskJson.schedules) {
            Assert.assertTrue(scheduleWrapper != null)

            Assert.assertTrue(!TextUtils.isEmpty(id))

            when {
                scheduleWrapper!!.singleScheduleJson != null -> {
                    Assert.assertTrue(scheduleWrapper.dailyScheduleJson == null)
                    Assert.assertTrue(scheduleWrapper.weeklyScheduleJson == null)
                    Assert.assertTrue(scheduleWrapper.monthlyDayScheduleJson == null)
                    Assert.assertTrue(scheduleWrapper.monthlyWeekScheduleJson == null)

                    remoteSingleScheduleRecords[id] = RemoteSingleScheduleRecord(id, this, scheduleWrapper)
                }
                scheduleWrapper.dailyScheduleJson != null -> {
                    Assert.assertTrue(scheduleWrapper.weeklyScheduleJson == null)
                    Assert.assertTrue(scheduleWrapper.monthlyDayScheduleJson == null)
                    Assert.assertTrue(scheduleWrapper.monthlyWeekScheduleJson == null)

                    remoteDailyScheduleRecords[id] = RemoteDailyScheduleRecord(id, this, scheduleWrapper)
                }
                scheduleWrapper.weeklyScheduleJson != null -> {
                    Assert.assertTrue(scheduleWrapper.monthlyDayScheduleJson == null)
                    Assert.assertTrue(scheduleWrapper.monthlyWeekScheduleJson == null)

                    remoteWeeklyScheduleRecords[id] = RemoteWeeklyScheduleRecord(id, this, scheduleWrapper)
                }
                scheduleWrapper.monthlyDayScheduleJson != null -> {
                    Assert.assertTrue(scheduleWrapper.monthlyWeekScheduleJson == null)

                    remoteMonthlyDayScheduleRecords[id] = RemoteMonthlyDayScheduleRecord(id, this, scheduleWrapper)
                }
                else -> {
                    Assert.assertTrue(scheduleWrapper.monthlyWeekScheduleJson != null)

                    remoteMonthlyWeekScheduleRecords[id] = RemoteMonthlyWeekScheduleRecord(id, this, scheduleWrapper)
                }
            }
        }
    }

    fun setEndTime(endTime: Long) {
        Assert.assertTrue(taskJson.endTime == null)

        taskJson.setEndTime(endTime)
        addValue("$key/endTime", endTime)
    }

    fun setOldestVisibleYear(oldestVisibleYear: Int) {
        if (oldestVisibleYear == taskJson.oldestVisibleYear)
            return

        taskJson.setOldestVisibleYear(oldestVisibleYear)
        addValue("$key/oldestVisibleYear", oldestVisibleYear)
    }

    fun setOldestVisibleMonth(oldestVisibleMonth: Int) {
        if (oldestVisibleMonth == taskJson.oldestVisibleMonth)
            return

        taskJson.setOldestVisibleMonth(oldestVisibleMonth)
        addValue("$key/oldestVisibleMonth", oldestVisibleMonth)
    }

    fun setOldestVisibleDay(oldestVisibleDay: Int) {
        if (oldestVisibleDay == taskJson.oldestVisibleDay)
            return

        taskJson.setOldestVisibleDay(oldestVisibleDay)
        addValue("$key/oldestVisibleDay", oldestVisibleDay)
    }

    override fun getValues(values: MutableMap<String, Any?>) {
        Assert.assertTrue(!deleted)
        Assert.assertTrue(!created)
        Assert.assertTrue(!updated)

        if (delete) {
            Log.e("asdf", "RemoteTaskRecord.getValues deleting " + this)

            Assert.assertTrue(!create)
            Assert.assertTrue(update != null)

            deleted = true
            values[key] = null
        } else if (create) {
            Log.e("asdf", "RemoteTaskRecord.getValues creating " + this)

            Assert.assertTrue(update == null)

            created = true

            values[key] = createObject
        } else {
            Assert.assertTrue(update != null)

            if (!update!!.isEmpty()) {
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

    fun newRemoteInstanceRecord(domainFactory: DomainFactory, instanceJson: InstanceJson, scheduleKey: ScheduleKey): RemoteInstanceRecord {
        val remoteInstanceRecord = RemoteInstanceRecord(true, domainFactory, this, instanceJson, scheduleKey)
        Assert.assertTrue(!_remoteInstanceRecords.containsKey(remoteInstanceRecord.scheduleKey))

        _remoteInstanceRecords[remoteInstanceRecord.scheduleKey] = remoteInstanceRecord
        return remoteInstanceRecord
    }

    fun newRemoteSingleScheduleRecord(scheduleWrapper: ScheduleWrapper): RemoteSingleScheduleRecord {
        val remoteSingleScheduleRecord = RemoteSingleScheduleRecord(this, scheduleWrapper)
        Assert.assertTrue(!remoteSingleScheduleRecords.containsKey(remoteSingleScheduleRecord.id))

        remoteSingleScheduleRecords[remoteSingleScheduleRecord.id] = remoteSingleScheduleRecord
        return remoteSingleScheduleRecord
    }

    fun newRemoteWeeklyScheduleRecord(scheduleWrapper: ScheduleWrapper): RemoteWeeklyScheduleRecord {
        val remoteWeeklyScheduleRecord = RemoteWeeklyScheduleRecord(this, scheduleWrapper)
        Assert.assertTrue(!remoteWeeklyScheduleRecords.containsKey(remoteWeeklyScheduleRecord.id))

        remoteWeeklyScheduleRecords[remoteWeeklyScheduleRecord.id] = remoteWeeklyScheduleRecord
        return remoteWeeklyScheduleRecord
    }

    fun newRemoteMonthlyDayScheduleRecord(scheduleWrapper: ScheduleWrapper): RemoteMonthlyDayScheduleRecord {
        val remoteMonthlyDayScheduleRecord = RemoteMonthlyDayScheduleRecord(this, scheduleWrapper)
        Assert.assertTrue(!remoteMonthlyDayScheduleRecords.containsKey(remoteMonthlyDayScheduleRecord.id))

        remoteMonthlyDayScheduleRecords[remoteMonthlyDayScheduleRecord.id] = remoteMonthlyDayScheduleRecord
        return remoteMonthlyDayScheduleRecord
    }

    fun newRemoteMonthlyWeekScheduleRecord(scheduleWrapper: ScheduleWrapper): RemoteMonthlyWeekScheduleRecord {
        val remoteMonthlyWeekScheduleRecord = RemoteMonthlyWeekScheduleRecord(this, scheduleWrapper)
        Assert.assertTrue(!remoteMonthlyWeekScheduleRecords.containsKey(remoteMonthlyWeekScheduleRecord.id))

        remoteMonthlyWeekScheduleRecords[remoteMonthlyWeekScheduleRecord.id] = remoteMonthlyWeekScheduleRecord
        return remoteMonthlyWeekScheduleRecord
    }
}
