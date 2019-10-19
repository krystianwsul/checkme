package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.ProjectJson
import com.krystianwsul.common.firebase.json.TaskHierarchyJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

@Suppress("LeakingThis")
abstract class RemoteProjectRecord<T : RemoteCustomTimeId, U : ProjectJson>(
        create: Boolean,
        val id: String,
        protected val projectJson: U
) : RemoteRecord(create) {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    abstract val remoteCustomTimeRecords: Map<out T, RemoteCustomTimeRecord<T, *>>

    val remoteTaskRecords by lazy {
        projectJson.tasks
                .mapValues { (id, taskJson) ->
                    check(id.isNotEmpty())

                    RemoteTaskRecord(id, this, taskJson)
                }
                .toMutableMap()
    }

    val remoteTaskHierarchyRecords by lazy {
        projectJson.taskHierarchies
                .mapValues { (id, taskHierarchyJson) ->
                    check(id.isNotEmpty())

                    RemoteTaskHierarchyRecord(id, this, taskHierarchyJson)
                }
                .toMutableMap()
    }

    override val key get() = id

    abstract val childKey: String

    var name by Committer(projectJson::name, "$id/$PROJECT_JSON")

    val startTime get() = projectJson.startTime

    var endTime by Committer(projectJson::endTime, "$id/$PROJECT_JSON")

    override val children
        get() = remoteTaskRecords.values +
                remoteTaskHierarchyRecords.values +
                remoteCustomTimeRecords.values

    fun newRemoteTaskRecord(taskJson: TaskJson): RemoteTaskRecord<T> {
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

    abstract fun getCustomTimeRecord(id: String): RemoteCustomTimeRecord<T, *>

    abstract fun getRemoteCustomTimeId(id: String): T

    abstract fun getRemoteCustomTimeKey(remoteCustomTimeId: T): CustomTimeKey<T>

    fun getRemoteCustomTimeKey(customTimeId: String) = getRemoteCustomTimeKey(getRemoteCustomTimeId(customTimeId))
}
