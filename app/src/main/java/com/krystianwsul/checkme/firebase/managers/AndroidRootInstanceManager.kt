package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.RootInstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey

class AndroidRootInstanceManager<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        snapshotInfos: List<SnapshotInfo>
) : RootInstanceManager<T>(taskRecord) {

    private fun SnapshotInfo.toRecord() = RootInstanceRecord(
            taskRecord,
            dataSnapshot.getValue(InstanceJson::class.java)!!,
            snapshotKey.dateKey,
            snapshotKey.timeKey,
            this@AndroidRootInstanceManager
    )

    override var rootInstanceRecords = snapshotInfos.map { it.toRecord() to false }
            .associateBy { it.first.instanceKey }
            .toMutableMap()

    override val databaseWrapper = AndroidDatabaseWrapper

    override fun getDatabaseCallback() = checkError("RootInstanceManager.save")

    fun newRootInstanceRecord(snapshotInfo: SnapshotInfo): RootInstanceRecord<T> {
        val rootInstanceRecord = snapshotInfo.toRecord()

        check(!rootInstanceRecords.containsKey(rootInstanceRecord.instanceKey))

        rootInstanceRecords[rootInstanceRecord.instanceKey] = Pair(rootInstanceRecord, false)

        return rootInstanceRecord
    }

    data class SnapshotKey(
            val dateKey: String,
            val timeKey: String
    ) {

        private val date by lazy { RootInstanceRecord.dateStringToDate(dateKey) }

        private fun getTimePair(projectRecord: ProjectRecord<*>) = RootInstanceRecord.timeStringToTime(
                projectRecord,
                timeKey
        ).first

        fun getScheduleKey(projectRecord: ProjectRecord<*>) = ScheduleKey(date, getTimePair(projectRecord))
    }

    fun clearSaved(instanceKey: InstanceKey) {
        val pair = rootInstanceRecords.getValue(instanceKey)
        check(pair.second)

        rootInstanceRecords[instanceKey] = Pair(pair.first, false)
    }

    data class SnapshotInfo(
            val snapshotKey: SnapshotKey,
            val dataSnapshot: DataSnapshot
    ) {

        constructor(
                dateKey: String,
                timeKey: String,
                dataSnapshot: DataSnapshot
        ) : this(SnapshotKey(dateKey, timeKey), dataSnapshot)
    }
}