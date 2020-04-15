package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.records.RootInstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectType

class AndroidRootInstanceManager<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        snapshotInfos: List<SnapshotInfo>,
        factoryProvider: FactoryProvider
) : RootInstanceManager<T>(taskRecord) {

    private fun SnapshotInfo.toRecord() = RootInstanceRecord(
            taskRecord,
            instanceJson,
            snapshotKey.dateKey,
            snapshotKey.timeKey,
            this@AndroidRootInstanceManager
    )

    override var rootInstanceRecords = snapshotInfos.map { it.toRecord() }
            .associateBy { it.instanceKey }
            .toMutableMap()

    override val databaseWrapper = factoryProvider.database

    override fun getDatabaseCallback() = checkError("RootInstanceManager.save")

    fun setSnapshotInfos(snapshotInfos: List<SnapshotInfo>) = if (isSaved) {
        isSaved = false

        ChangeType.LOCAL
    } else {
        rootInstanceRecords = snapshotInfos.map { it.toRecord() }
                .associateBy { it.instanceKey }
                .toMutableMap()

        ChangeType.REMOTE
    }

    data class SnapshotKey(val dateKey: String, val timeKey: String)

    data class SnapshotInfo(val snapshotKey: SnapshotKey, val instanceJson: InstanceJson) {

        constructor(
                dateKey: String,
                timeKey: String,
                instanceJson: InstanceJson
        ) : this(SnapshotKey(dateKey, timeKey), instanceJson)
    }
}