package com.krystianwsul.common.firebase.records

import com.badoo.reaktive.subject.behavior.BehaviorSubject
import com.krystianwsul.common.firebase.json.ProjectJson
import com.krystianwsul.common.firebase.json.TaskHierarchyJson
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.*

@Suppress("LeakingThis")
abstract class ProjectRecord<T : ProjectType>(
        create: Boolean,
        private val projectJson: ProjectJson<T>,
        private val _id: ProjectKey<T>,
        committerKey: String,
) : RemoteRecord(create), JsonTime.ProjectIdProvider<T> {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    abstract val projectKey: ProjectKey<T>

    abstract val customTimeRecords: Map<out CustomTimeId.Project<T>, ProjectCustomTimeRecord<T>>

    abstract val taskRecordsRelay: BehaviorSubject<out Map<String, TaskRecord<T>>>

    abstract val taskRecords: Map<String, TaskRecord<T>>

    lateinit var taskHierarchyRecords: MutableMap<String, TaskHierarchyRecord>
        private set

    protected fun initTaskHierarchyRecords() {
        taskHierarchyRecords = projectJson.taskHierarchies
                .mapValues { (id, taskHierarchyJson) ->
                    check(id.isNotEmpty())

                    TaskHierarchyRecord(id, this, taskHierarchyJson)
                }
                .toMutableMap()
    }

    override val key get() = _id.key

    abstract val childKey: String

    var name by Committer(projectJson::name, "$key/$PROJECT_JSON")

    val startTime get() = projectJson.startTime
    var startTimeOffset by Committer(projectJson::startTimeOffset, committerKey)

    var endTime by Committer(projectJson::endTime, committerKey)
    var endTimeOffset by Committer(projectJson::endTimeOffset, committerKey)

    override val children
        get() = taskRecords.values +
                taskHierarchyRecords.values +
                customTimeRecords.values

    fun newTaskHierarchyRecord(taskHierarchyJson: TaskHierarchyJson): TaskHierarchyRecord {
        val taskHierarchyRecord = TaskHierarchyRecord(this, taskHierarchyJson)
        check(!taskHierarchyRecords.containsKey(taskHierarchyRecord.id))

        taskHierarchyRecords[taskHierarchyRecord.id] = taskHierarchyRecord
        return taskHierarchyRecord
    }

    abstract fun getTaskHierarchyRecordId(): String

    abstract fun getTaskRecordId(): String

    abstract fun getScheduleRecordId(taskId: String): String

    abstract fun getCustomTimeRecord(id: String): ProjectCustomTimeRecord<T>

    abstract fun getCustomTimeKey(customTimeId: CustomTimeId.Project<T>): CustomTimeKey.Project<T>

    abstract fun newNoScheduleOrParentRecordId(taskId: String): String

    fun getCustomTimeKey(customTimeId: String) = getCustomTimeKey(getCustomTimeId(customTimeId))

    fun getTaskKey(taskId: String) = TaskKey(projectKey, taskId)
}
