package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.records.RootInstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectType

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

    data class SnapshotKey(
            val dateKey: String,
            val timeKey: String
    )

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