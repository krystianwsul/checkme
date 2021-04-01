package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.GenericTypeIndicator
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.snapshot.IndicatorSnapshot
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.records.RootInstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType

class AndroidRootInstanceManager<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        snapshot: IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>?,
        factoryProvider: FactoryProvider,
) :
        RootInstanceManager<T>(taskRecord, snapshot.toSnapshotInfos(), factoryProvider.database),
        SnapshotRecordManager<MutableMap<InstanceKey, RootInstanceRecord<T>>, IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>> {

    companion object {

        private val typeToken = object : GenericTypeIndicator<Map<String, Map<String, InstanceJson>>>() {}

        private fun IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>?.toSnapshotInfos() = this?.getValue(typeToken)
                ?.map { (dateString, timeMap) ->
                    timeMap.map { (timeString, instanceJson) -> SnapshotInfo(dateString, timeString, instanceJson) }
                }
                ?.flatten()
                ?: listOf()
    }

    override fun set(snapshot: IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>) = set {
        snapshot.toSnapshotInfos()
                .map { it.toRecord() }
                .associateBy { it.instanceKey }
                .toMutableMap()
    }
}