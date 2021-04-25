package com.krystianwsul.common.firebase.records.project

import com.krystianwsul.common.firebase.json.projects.ProjectJson
import com.krystianwsul.common.firebase.json.taskhierarchies.ProjectTaskHierarchyJson
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.RootTaskParentDelegate
import com.krystianwsul.common.firebase.records.customtime.ProjectCustomTimeRecord
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.firebase.records.taskhierarchy.ProjectTaskHierarchyRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.*

@Suppress("LeakingThis")
abstract class ProjectRecord<T : ProjectType>(
        create: Boolean,
        private val projectJson: ProjectJson<T>,
        private val _id: ProjectKey<T>,
        committerKey: String,
) : RemoteRecord(create), JsonTime.ProjectCustomTimeIdAndKeyProvider, TaskRecord.Parent {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    abstract val projectKey: ProjectKey<T>

    abstract val customTimeRecords: Map<out CustomTimeId.Project, ProjectCustomTimeRecord<T>>

    abstract val taskRecords: Map<String, ProjectTaskRecord>

    lateinit var taskHierarchyRecords: MutableMap<String, ProjectTaskHierarchyRecord>
        private set

    protected fun initTaskHierarchyRecords() {
        taskHierarchyRecords = projectJson.taskHierarchies
                .mapValues { (id, taskHierarchyJson) ->
                    check(id.isNotEmpty())

                    ProjectTaskHierarchyRecord(id, this, taskHierarchyJson)
                }
                .toMutableMap()
    }

    override val key get() = _id.key

    abstract val childKey: String

    var name by Committer(projectJson::name, committerKey)

    val startTime get() = projectJson.startTime
    var startTimeOffset by Committer(projectJson::startTimeOffset, committerKey)

    var endTime by Committer(projectJson::endTime, committerKey)
    var endTimeOffset by Committer(projectJson::endTimeOffset, committerKey)

    override val children
        get() = taskRecords.values +
                taskHierarchyRecords.values +
                customTimeRecords.values

    val rootTaskParentDelegate = object : RootTaskParentDelegate(projectJson) {

        override fun addValue(subKey: String, value: Boolean?) {
            this@ProjectRecord.addValue("$committerKey/$subKey", value)
        }
    }

    fun newTaskHierarchyRecord(taskHierarchyJson: ProjectTaskHierarchyJson): ProjectTaskHierarchyRecord {
        val taskHierarchyRecord = ProjectTaskHierarchyRecord(this, taskHierarchyJson)
        check(!taskHierarchyRecords.containsKey(taskHierarchyRecord.id))

        taskHierarchyRecords[taskHierarchyRecord.id] = taskHierarchyRecord
        return taskHierarchyRecord
    }

    abstract fun getProjectTaskHierarchyRecordId(): String
    abstract fun newNestedTaskHierarchyRecordId(taskId: String): String

    abstract fun getTaskRecordId(): String

    abstract fun getScheduleRecordId(taskId: String): String

    abstract fun getCustomTimeRecord(id: String): ProjectCustomTimeRecord<T>

    abstract fun newNoScheduleOrParentRecordId(taskId: String): String

    abstract override fun getProjectCustomTimeKey(projectCustomTimeId: CustomTimeId.Project): CustomTimeKey.Project<T>
    fun getProjectCustomTimeKey(customTimeId: String) = getProjectCustomTimeKey(getProjectCustomTimeId(customTimeId))

    fun getTaskKey(taskId: String) = TaskKey.Project(projectKey, taskId) // todo task after project
}
