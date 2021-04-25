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
        private val parent: Parent,
        private val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
) : Task(
        CopyScheduleHelper.Root,
        JsonTime.CustomTimeProvider.getForRootTask(userCustomTimeProvider),
        taskRecord,
        ParentTaskDelegate.Root(parent),
        parent,
) {

    private val projectProperty = invalidatableLazy {
        val interval = intervals.last()

        when (val type = interval.type) {
            is Type.Schedule ->
                parent.getProject(type.taskParentEntries.maxByOrNull { it.startExactTimeStamp }!!.projectId)
            is Type.NoSchedule -> parent.getProject(type.noScheduleOrParent!!.projectId)
            is Type.Child -> type.parentTaskHierarchy
                    .parentTask
                    .project
        }
    }.apply { addTo(intervalsProperty) }

    override val project by projectProperty

    override val taskKey get() = TaskKey.Root(taskRecord.id)

    override val projectParentTaskHierarchies = setOf<ProjectTaskHierarchy>()

    override fun createChildTask(
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double?,
    ) = TODO("todo task edit")

    override fun deleteFromParent() = parent.deleteTask(this)

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

    override fun addChild(childTask: Task, now: ExactTimeStamp.Local) =
            createParentNestedTaskHierarchy(childTask, now)

    override fun invalidateProjectParentTaskHierarchies() = invalidateIntervals()

    override fun updateProject(
            projectUpdater: ProjectUpdater,
            now: ExactTimeStamp.Local,
            projectId: ProjectKey<*>,
    ): Task {
        return if (projectId == project.projectKey) // todo task edit
            this
        else
            projectUpdater.convert(now, this, projectId)
    }

    interface Parent : Task.Parent {

        fun deleteTask(task: RootTask)

        fun getTask(taskKey: TaskKey.Root): RootTask

        fun getProject(projectId: String): Project<*>
    }
}
