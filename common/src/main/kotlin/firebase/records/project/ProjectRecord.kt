package com.krystianwsul.common.firebase.records.project

import com.krystianwsul.common.firebase.json.projects.ProjectJson
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.RootTaskParentDelegate
import com.krystianwsul.common.firebase.records.customtime.ProjectCustomTimeRecord
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.firebase.records.taskhierarchy.ProjectTaskHierarchyRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.*

@Suppress("LeakingThis")
sealed class ProjectRecord<T : ProjectType>(
    create: Boolean,
    private val projectJson: ProjectJson<T>,
    private val _id: ProjectKey<T>,
    protected val committerKey: String,
) : RemoteRecord(create), JsonTime.ProjectCustomTimeIdAndKeyProvider, TaskRecord.Parent {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    abstract val projectKey: ProjectKey<T>

    abstract val customTimeRecords: Map<out CustomTimeId.Project, ProjectCustomTimeRecord<T>>

    abstract val taskRecords: Map<String, ProjectTaskRecord>

    lateinit var taskHierarchyRecords: MutableMap<TaskHierarchyId, ProjectTaskHierarchyRecord>
        private set

    protected fun initTaskHierarchyRecords() {
        taskHierarchyRecords = projectJson.taskHierarchies
            .entries
            .associate { (untypedId, taskHierarchyJson) ->
                check(untypedId.isNotEmpty())

                val typedId = TaskHierarchyId(untypedId)

                typedId to ProjectTaskHierarchyRecord(typedId, this, taskHierarchyJson)
            }
            .toMutableMap()
    }

    override val key get() = _id.key

    abstract val childKey: String

    val startTime get() = projectJson.startTime
    var startTimeOffset by Committer(projectJson::startTimeOffset, committerKey)

    override val children
        get() = taskRecords.values +
                taskHierarchyRecords.values +
                customTimeRecords.values

    val rootTaskParentDelegate = object : RootTaskParentDelegate(projectJson) {

        override fun addValue(subKey: String, value: Boolean?) {
            this@ProjectRecord.addValue("$committerKey/$subKey", value)
        }
    }

    abstract fun getCustomTimeRecord(id: String): ProjectCustomTimeRecord<T>

    abstract override fun getProjectCustomTimeKey(projectCustomTimeId: CustomTimeId.Project): CustomTimeKey.Project<T>
    fun getProjectCustomTimeKey(customTimeId: String) = getProjectCustomTimeKey(getProjectCustomTimeId(customTimeId))

    fun getTaskKey(taskId: String) = TaskKey.Project(projectKey, taskId)
}
