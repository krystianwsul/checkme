package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.ProjectJson
import com.krystianwsul.common.firebase.json.TaskHierarchyJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

@Suppress("LeakingThis")
abstract class RemoteProjectRecord<T : RemoteCustomTimeId, U : ProjectJson, V : ProjectKey>(
        create: Boolean,
        val id: V,
        protected val projectJson: U
) : RemoteRecord(create) {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    abstract val customTimeRecords: Map<out T, CustomTimeRecord<T, *>>

    val remoteTaskRecords = projectJson.tasks
            .mapValues { (id, taskJson) ->
                check(id.isNotEmpty())

                RemoteTaskRecord(id, this, taskJson)
            }
            .toMutableMap()

    val remoteTaskHierarchyRecords by lazy {
        projectJson.taskHierarchies
                .mapValues { (id, taskHierarchyJson) ->
                    check(id.isNotEmpty())

                    RemoteTaskHierarchyRecord(id, this, taskHierarchyJson)
                }
                .toMutableMap()
    }

    override val key get() = id.key

    abstract val childKey: String

    var name by Committer(projectJson::name, "$key/$PROJECT_JSON")

    val startTime get() = projectJson.startTime

    var endTime by Committer(projectJson::endTime, "$key/$PROJECT_JSON")

    override val children
        get() = remoteTaskRecords.values +
                remoteTaskHierarchyRecords.values +
                customTimeRecords.values

    fun newRemoteTaskRecord(taskJson: TaskJson): RemoteTaskRecord<T, V> {
        val remoteTaskRecord = RemoteTaskRecord(this, taskJson)
        check(!remoteTaskRecords.containsKey(remoteTaskRecord.id))

        remoteTaskRecords[remoteTaskRecord.id] = remoteTaskRecord
        return remoteTaskRecord
    }

    fun newRemoteTaskHierarchyRecord(taskHierarchyJson: TaskHierarchyJson): RemoteTaskHierarchyRecord {
        val remoteTaskHierarchyRecord = RemoteTaskHierarchyRecord(this, taskHierarchyJson)
        check(!remoteTaskHierarchyRecords.containsKey(remoteTaskHierarchyRecord.id))

        remoteTaskHierarchyRecords[remoteTaskHierarchyRecord.id] = remoteTaskHierarchyRecord
        return remoteTaskHierarchyRecord
    }

    abstract fun getTaskHierarchyRecordId(): String

    abstract fun getTaskRecordId(): String

    abstract fun getScheduleRecordId(taskId: String): String

    abstract fun getCustomTimeRecord(id: String): CustomTimeRecord<T, *>

    abstract fun getRemoteCustomTimeId(id: String): T

    abstract fun getRemoteCustomTimeKey(remoteCustomTimeId: T): CustomTimeKey<T, V>

    fun getRemoteCustomTimeKey(customTimeId: String) = getRemoteCustomTimeKey(getRemoteCustomTimeId(customTimeId))
}
