package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectType

class AndroidRootInstanceManager<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        snapshotInfos: List<SnapshotInfo>,
        factoryProvider: FactoryProvider
) : RootInstanceManager<T>(taskRecord, snapshotInfos, factoryProvider.database) {

    fun setSnapshotInfos(snapshotInfos: List<SnapshotInfo>) = if (isSaved) {
        isSaved = false

        ChangeType.LOCAL
    } else {
        value = snapshotInfos.map { it.toRecord() }
                .associateBy { it.instanceKey }
                .toMutableMap()

        ChangeType.REMOTE
    }
}