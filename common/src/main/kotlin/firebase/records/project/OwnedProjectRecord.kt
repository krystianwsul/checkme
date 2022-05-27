package com.krystianwsul.common.firebase.records.project

import com.krystianwsul.common.firebase.json.projects.OwnedProjectJson
import com.krystianwsul.common.firebase.records.customtime.ProjectCustomTimeRecord
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.firebase.records.taskhierarchy.ProjectTaskHierarchyRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.*

@Suppress("LeakingThis")
sealed class OwnedProjectRecord<T : ProjectType>(
    create: Boolean,
    private val projectJson: OwnedProjectJson,
    _id: ProjectKey<T>,
    committerKey: String,
) : ProjectRecord<T>(create, projectJson, _id, committerKey), JsonTime.ProjectCustomTimeIdAndKeyProvider, TaskRecord.Parent {

    abstract val customTimeRecords: Map<out CustomTimeId.Project, ProjectCustomTimeRecord<T>>

    override abstract val taskRecords: Map<String, ProjectTaskRecord>

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

    val startTime get() = projectJson.startTime
    var startTimeOffset by Committer(projectJson::startTimeOffset, committerKey)

    override val children
        get() = taskRecords.values +
                taskHierarchyRecords.values +
                customTimeRecords.values

    abstract fun getCustomTimeRecord(id: String): ProjectCustomTimeRecord<T>

    abstract override fun getProjectCustomTimeKey(projectCustomTimeId: CustomTimeId.Project): CustomTimeKey.Project<T>
    fun getProjectCustomTimeKey(customTimeId: String) = getProjectCustomTimeKey(getProjectCustomTimeId(customTimeId))

    fun getTaskKey(taskId: String) = TaskKey.Project(projectKey, taskId)
}
