package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.records.RootInstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType

class AndroidRootInstanceManager<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        snapshot: Snapshot<Map<String, Map<String, InstanceJson>>>?,
        factoryProvider: FactoryProvider,
) :
        RootInstanceManager<T>(taskRecord, snapshot.toSnapshotInfos(), factoryProvider.database),
        SnapshotRecordManager<MutableMap<InstanceKey, RootInstanceRecord<T>>, Snapshot<Map<String, Map<String, InstanceJson>>>> {

    companion object {

        private fun Snapshot<Map<String, Map<String, InstanceJson>>>?.toSnapshotInfos() = this?.value
                ?.map { (dateString, timeMap) ->
                    timeMap.map { (timeString, instanceJson) -> SnapshotInfo(dateString, timeString, instanceJson) }
                }
                ?.flatten()
                ?: listOf()
    }

    override fun set(snapshot: Snapshot<Map<String, Map<String, InstanceJson>>>) = set {
        snapshot.toSnapshotInfos()
                .map { it.toRecord() }
                .associateBy { it.instanceKey }
                .toMutableMap()
    }
}