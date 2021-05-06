package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.tasks.PrivateTaskJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.CopyScheduleHelper
import com.krystianwsul.common.firebase.models.ProjectUser
import com.krystianwsul.common.firebase.models.customtime.PrivateCustomTime
import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.PrivateProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

class PrivateProject(
        override val projectRecord: PrivateProjectRecord,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
        rootTaskProvider: RootTaskProvider,
) : Project<ProjectType.Private>(
        CopyScheduleHelper.Private,
        AssignedToHelper.Private,
        userCustomTimeProvider,
        rootTaskProvider,
) {

    override val projectKey = projectRecord.projectKey

    override val remoteCustomTimes = HashMap<CustomTimeId.Project.Private, PrivateCustomTime>()
    override val _tasks: MutableMap<String, ProjectTask>
    override val taskHierarchyContainer = TaskHierarchyContainer()

    override val customTimes get() = remoteCustomTimes.values

    var defaultTimesCreated
        get() = projectRecord.defaultTimesCreated
        set(value) {
            projectRecord.defaultTimesCreated = value
        }

    init {
        for (remoteCustomTimeRecord in projectRecord.customTimeRecords.values) {
            @Suppress("LeakingThis")
            val remoteCustomTime = PrivateCustomTime(this, remoteCustomTimeRecord)

            remoteCustomTimes[remoteCustomTime.id] = remoteCustomTime
        }

        _tasks = projectRecord.taskRecords
                .values
                .map { ProjectTask(this, it) }
                .associateBy { it.id }
                .toMutableMap()

        projectRecord.taskHierarchyRecords
                .values
                .map { ProjectTaskHierarchy(this, it) }
                .forEach { taskHierarchyContainer.add(it.id, it) }

        initializeInstanceHierarchyContainers()
    }

    override fun deleteCustomTime(remoteCustomTime: Time.Custom.Project<ProjectType.Private>) {
        check(remoteCustomTimes.containsKey(remoteCustomTime.id))

        remoteCustomTimes.remove(remoteCustomTime.id)
    }

    override fun getProjectCustomTime(projectCustomTimeId: CustomTimeId.Project): PrivateCustomTime {
        check(remoteCustomTimes.containsKey(projectCustomTimeId as CustomTimeId.Project.Private))

        return remoteCustomTimes.getValue(projectCustomTimeId)
    }

    override fun getProjectCustomTime(projectCustomTimeKey: CustomTimeKey.Project<ProjectType.Private>): PrivateCustomTime =
            getProjectCustomTime(projectCustomTimeKey.customTimeId)

    override fun createChildTask(
            parentTask: ProjectTask,
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double?,
    ): ProjectTask {
        val taskJson = PrivateTaskJson(
                name,
                now.long,
                now.offset,
                null,
                note,
                image = image,
                ordinal = ordinal
        )

        val childTask = newTask(taskJson)

        createTaskHierarchy(parentTask, childTask, now)

        return childTask
    }

    override fun copyTaskRecord(
            oldTask: ProjectTask,
            now: ExactTimeStamp.Local,
            instanceJsons: MutableMap<String, InstanceJson>,
    ) = projectRecord.newTaskRecord(PrivateTaskJson(
            oldTask.name,
            now.long,
            now.offset,
            oldTask.endExactTimeStamp?.long,
            oldTask.note,
            instanceJsons,
            ordinal = oldTask.ordinal
    ))

    fun newTask(taskJson: PrivateTaskJson): ProjectTask {
        val taskRecord = projectRecord.newTaskRecord(taskJson)

        val task = ProjectTask(this, taskRecord)
        check(!_tasks.containsKey(task.id))

        _tasks[task.id] = task

        return task
    }

    override fun createTask(
            now: ExactTimeStamp.Local,
            image: TaskJson.Image?,
            name: String,
            note: String?,
            ordinal: Double?,
    ) = newTask(PrivateTaskJson(
            name,
            now.long,
            now.offset,
            note = note,
            image = image,
            ordinal = ordinal
    ))

    override fun getAssignedTo(userKeys: Set<UserKey>) = mapOf<UserKey, ProjectUser>()
}