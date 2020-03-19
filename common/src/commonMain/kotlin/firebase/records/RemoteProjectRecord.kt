package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.ProjectJson
import com.krystianwsul.common.firebase.json.TaskHierarchyJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

@Suppress("LeakingThis")
abstract class RemoteProjectRecord<T : ProjectType>(
        create: Boolean,
        private val projectJson: ProjectJson,
        private val _id: ProjectKey<T> // bo musi być dostępne w konstruktorze, a abstract nie jest jeszcze zinicjalizowane
) : RemoteRecord(create) {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    abstract val id: ProjectKey<T>

    abstract val customTimeRecords: Map<out CustomTimeId<T>, CustomTimeRecord<T>>

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

    override val key get() = _id.key

    abstract val childKey: String

    var name by Committer(projectJson::name, "$key/$PROJECT_JSON")

    val startTime get() = projectJson.startTime

    var endTime by Committer(projectJson::endTime, "$key/$PROJECT_JSON")

    override val children
        get() = remoteTaskRecords.values +
                remoteTaskHierarchyRecords.values +
                customTimeRecords.values

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

    abstract fun getCustomTimeRecord(id: String): CustomTimeRecord<T>

    abstract fun getCustomTimeId(id: String): CustomTimeId<T>

    abstract fun getRemoteCustomTimeKey(customTimeId: CustomTimeId<T>): CustomTimeKey<T>

    fun getRemoteCustomTimeKey(customTimeId: String) = getRemoteCustomTimeKey(getCustomTimeId(customTimeId))
}
