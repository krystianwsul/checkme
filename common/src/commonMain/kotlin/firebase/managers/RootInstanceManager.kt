package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.records.RootInstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey
import kotlinx.serialization.Serializable

open class RootInstanceManager<T : ProjectType>(
        protected val taskRecord: TaskRecord<T>,
        snapshotInfos: List<SnapshotInfo>,
        val databaseWrapper: DatabaseWrapper
) : ValueRecordManager<MutableMap<InstanceKey, RootInstanceRecord<T>>>(), RootInstanceRecord.Parent {

    protected fun SnapshotInfo.toRecord() = RootInstanceRecord(
            taskRecord,
            instanceJson,
            snapshotKey.dateKey,
            snapshotKey.timeKey,
            this@RootInstanceManager
    )

    override var value = snapshotInfos.map { it.toRecord() }
            .associateBy { it.instanceKey }
            .toMutableMap()

    override val records get() = value.values

    override val databasePrefix = "${DatabaseWrapper.KEY_INSTANCES}/${taskRecord.rootInstanceKey}"

    fun newRootInstanceRecord(
            instanceJson: InstanceJson,
            scheduleKey: ScheduleKey,
    ) = RootInstanceRecord(
            taskRecord,
            instanceJson,
            scheduleKey,
            this,
    ).also {
        check(!value.containsKey(it.instanceKey))

        value[it.instanceKey] = it
    }

    override fun removeRootInstanceRecord(instanceKey: InstanceKey) {
        check(value.containsKey(instanceKey))

        value.remove(instanceKey)
    }

    @Serializable
    data class SnapshotKey(val dateKey: String, val timeKey: String)

    @Serializable
    data class SnapshotInfo(val snapshotKey: SnapshotKey, val instanceJson: InstanceJson) {

        constructor(
                dateKey: String,
                timeKey: String,
                instanceJson: InstanceJson
        ) : this(SnapshotKey(dateKey, timeKey), instanceJson)
    }
}