package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.common.firebase.ChangeType
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

    fun setSnapshotInfos(snapshotInfos: List<SnapshotInfo>) = if (isSaved) {
        isSaved = false

        ChangeType.LOCAL
    } else {
        rootInstanceRecords = snapshotInfos.map { it.toRecord() }
                .associateBy { it.instanceKey }
                .toMutableMap()

        ChangeType.REMOTE
    }
}