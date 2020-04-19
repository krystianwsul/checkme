package firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.records.RootInstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectType

class JsRootInstanceManager<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        snapshotInfos: List<SnapshotInfo>,
        override val databaseWrapper: DatabaseWrapper
) : RootInstanceManager<T>(taskRecord) {

    private fun SnapshotInfo.toRecord() = RootInstanceRecord(
            taskRecord,
            instanceJson,
            snapshotKey.dateKey,
            snapshotKey.timeKey,
            this@JsRootInstanceManager
    )

    override var rootInstanceRecords = snapshotInfos.map { it.toRecord() } // todo instances move into RootInstanceManager
            .associateBy { it.instanceKey }
            .toMutableMap()
}