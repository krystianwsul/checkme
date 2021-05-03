package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.CopyScheduleHelper
import com.krystianwsul.common.firebase.models.interval.Type
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

class RootTask(
        val taskRecord: RootTaskRecord,
        override val parent: Parent,
        private val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
) : Task(
        CopyScheduleHelper.Root,
        JsonTime.CustomTimeProvider.getForRootTask(userCustomTimeProvider),
        taskRecord,
        ParentTaskDelegate.Root(parent),
) {

    private fun Type.Schedule.getParentProjectSchedule() = taskParentEntries.maxByOrNull { it.startExactTimeStamp }!!

    private val projectIdProperty = invalidatableLazy {
        val interval = intervals.last()

        when (val type = interval.type) {
            is Type.Schedule -> type.getParentProjectSchedule().projectId
            is Type.NoSchedule -> type.noScheduleOrParent!!.projectId
            is Type.Child -> {
                val parentTask = type.parentTaskHierarchy.parentTask as RootTask

                parentTask.projectId
            }
        }
    }.apply { addTo(intervalsProperty) }

    val projectId: String by projectIdProperty

    override val project get() = parent.getProject(projectId)

    override val taskKey get() = TaskKey.Root(taskRecord.id)

    override val projectParentTaskHierarchies = setOf<ProjectTaskHierarchy>()

    override val projectCustomTimeIdProvider = JsonTime.ProjectCustomTimeIdProvider.rootTask

    override val addProjectIdToNoScheduleOrParent = true

    override fun createChildTask(
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double?,
    ): RootTask {
        val childTask = parent.createTask(now, image, name, note, ordinal)

        childTask.createParentNestedTaskHierarchy(this, now)
        addRootTask(childTask)

        return childTask
    }

    override fun deleteProjectRootTaskId() = project.removeRootTask(taskKey)
    override fun deleteFromParent() = parent.deleteRootTask(this)

    override fun getDateTime(scheduleKey: ScheduleKey) =
            DateTime(scheduleKey.scheduleDate, getTime(scheduleKey.scheduleTimePair))

    private fun getTime(timePair: TimePair) = timePair.customTimeKey
            ?.let { userCustomTimeProvider.getUserCustomTime(it as CustomTimeKey.User) }
            ?: Time.Normal(timePair.hourMinute!!)

    override fun getOrCopyTime(
            ownerKey: UserKey,
            dayOfWeek: DayOfWeek,
            time: Time,
            customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
            now: ExactTimeStamp.Local,
    ) = when (time) {
        is Time.Custom.Project<*> -> {
            check(Time.Custom.User.WRITE_USER_CUSTOM_TIMES)

            customTimeMigrationHelper.tryMigrateProjectCustomTime(time, now)
                    ?: Time.Normal(time.getHourMinute(dayOfWeek))
        }
        is Time.Custom.User -> time
        is Time.Normal -> time
    }

    override fun addChild(childTask: Task, now: ExactTimeStamp.Local): TaskHierarchyKey {
        val taskHierarchyKey = childTask.createParentNestedTaskHierarchy(this, now)
        addRootTask(childTask as RootTask)

        return taskHierarchyKey
    }

    fun addRootTask(childTask: RootTask) {
        taskRecord.rootTaskParentDelegate.addRootTask(childTask.taskKey) { parent.updateTaskRecord(taskKey, it) }
    }

    override fun invalidateProjectParentTaskHierarchies() = invalidateIntervals()

    override fun updateProject(
            projectUpdater: ProjectUpdater,
            now: ExactTimeStamp.Local,
            projectKey: ProjectKey<*>,
    ): Task {
        if (project.projectKey == projectKey) return this

        val interval = intervals.last()

        val taskParentEntry = when (val type = interval.type) {
            is Type.Schedule -> type.getParentProjectSchedule()
            is Type.NoSchedule -> type.noScheduleOrParent!!
            is Type.Child -> throw UnsupportedOperationException()
        }

        val oldProject = project

        taskParentEntry.updateProject(projectKey)

        parent.updateProject(taskKey, oldProject, projectKey)

        return this
    }

    override fun addRootTaskIdToProject() = project.addRootTask(taskKey)

    interface Parent : Task.Parent, Project.RootTaskProvider {

        fun deleteRootTask(task: RootTask)

        fun getProject(projectId: String): Project<*>

        override fun createTask(
                now: ExactTimeStamp.Local,
                image: TaskJson.Image?,
                name: String,
                note: String?,
                ordinal: Double?,
        ): RootTask

        fun updateProject(taskKey: TaskKey.Root, oldProject: Project<*>, newProjectKey: ProjectKey<*>)
    }
}
