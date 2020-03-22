package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.ProjectJson
import com.krystianwsul.common.firebase.json.TaskHierarchyJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.utils.*

@Suppress("LeakingThis")
abstract class ProjectRecord<T : ProjectType>(
        create: Boolean,
        private val projectJson: ProjectJson,
        private val _id: ProjectKey<T> // bo musi być dostępne w konstruktorze, a abstract nie jest jeszcze zinicjalizowane
) : RemoteRecord(create) {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    abstract val projectKey: ProjectKey<T>

    abstract val customTimeRecords: Map<out CustomTimeId<T>, CustomTimeRecord<T>>

    val taskRecords by lazy {
        projectJson.tasks
                .mapValues { (id, taskJson) ->
                    check(id.isNotEmpty())

                    TaskRecord(id, this, taskJson)
                }
                .toMutableMap()
    }

    val taskHierarchyRecords by lazy {
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
        get() = taskRecords.values +
                taskHierarchyRecords.values +
                customTimeRecords.values

    fun newTaskRecord(taskJson: TaskJson): TaskRecord<T> {
        val remoteTaskRecord = TaskRecord(this, taskJson)
        check(!taskRecords.containsKey(remoteTaskRecord.id))

        taskRecords[remoteTaskRecord.id] = remoteTaskRecord
        return remoteTaskRecord
    }

    fun newRemoteTaskHierarchyRecord(taskHierarchyJson: TaskHierarchyJson): RemoteTaskHierarchyRecord {
        val remoteTaskHierarchyRecord = RemoteTaskHierarchyRecord(this, taskHierarchyJson)
        check(!taskHierarchyRecords.containsKey(remoteTaskHierarchyRecord.id))

        taskHierarchyRecords[remoteTaskHierarchyRecord.id] = remoteTaskHierarchyRecord
        return remoteTaskHierarchyRecord
    }

    abstract fun getTaskHierarchyRecordId(): String

    abstract fun getTaskRecordId(): String

    abstract fun getScheduleRecordId(taskId: String): String

    abstract fun getCustomTimeRecord(id: String): CustomTimeRecord<T>

    abstract fun getCustomTimeId(id: String): CustomTimeId<T>

    abstract fun getRemoteCustomTimeKey(customTimeId: CustomTimeId<T>): CustomTimeKey<T>

    fun getRemoteCustomTimeKey(customTimeId: String) = getRemoteCustomTimeKey(getCustomTimeId(customTimeId))

    fun getTaskKey(taskId: String) = TaskKey(this.projectKey, taskId)
}
