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
        children: List<SnapshotInfo>
) : RootInstanceManager<T>(taskRecord) {

    private fun SnapshotInfo.toRecord() = RootInstanceRecord(
            taskRecord,
            dataSnapshot.getValue(InstanceJson::class.java)!!,
            dateKey,
            timeKey,
            this@AndroidRootInstanceManager
    )

    override var rootInstanceRecords = children.map { it.toRecord() to false }
            .associateBy { it.first.instanceKey }
            .toMutableMap()

    override val databaseWrapper = AndroidDatabaseWrapper

    override fun getDatabaseCallback() = checkError("RootInstanceManager.save")

    data class SnapshotInfo(
            val dateKey: String,
            val timeKey: String,
            val dataSnapshot: DataSnapshot
    )
}